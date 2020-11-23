package webserver.graph;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.EdgeTriplet;
import org.apache.spark.graphx.Graph;
import org.apache.spark.storage.StorageLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Option;
import scala.Predef;
import scala.Tuple2;
import scala.reflect.ClassTag$;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import scala.runtime.AbstractFunction3;
import webserver.util.FileUtil;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlayerHeroGraph implements Serializable {
    static abstract class SerializableFunction1<T1, R> extends AbstractFunction1<T1, R> implements Serializable {
    }

    static abstract class SerializableFunction2<T1, T2, R> extends AbstractFunction2<T1, T2, R> implements Serializable {
    }

    static abstract class SerializableFunction3<T1, T2, T3, R> extends AbstractFunction3<T1, T2, T3, R> implements Serializable {
    }

    @Autowired
    private transient JavaSparkContext sc;

    public static Integer lowestMatches = 35; // 限制35场比赛以上，不然前端画不动

    private Map<Long, String> players;

    private Map<Long, String> heroes;

    private Graph<JSONObject, JSONObject> graph;

    public void buildGraph() {
        List<Tuple2<Object, JSONObject>> vertices = new ArrayList<>();
        List<Edge<JSONObject>> edges = new ArrayList<>();
        Map<String, Long> players = new HashMap<>();
        Map<String, Long> heroes = new HashMap<>();
        JSONArray jsonEdges = JSON.parseArray(FileUtil.readFile(FileUtil.PLAYER_HERO_EDGE_PATH));
        long id = 1L;
        for (int i = 0; i < jsonEdges.size(); i++) {
            JSONObject edge = jsonEdges.getJSONObject(i);
            String player = edge.getString("player");
            String hero = edge.getString("hero");
            Integer wins = edge.getInteger("wins");
            Integer matches = edge.getInteger("matches");
            if (!players.containsKey(player)) {
                JSONObject vattr = new JSONObject();
                vattr.put("name", player);
                vattr.put("type", "player");
                players.put(player, id);
                vertices.add(new Tuple2<>(id, vattr));
                id++;
            }
            if (!heroes.containsKey(hero)) {
                JSONObject vattr = new JSONObject();
                vattr.put("name", hero);
                vattr.put("type", "hero");
                heroes.put(hero, id);
                vertices.add(new Tuple2<>(id, vattr));
                id++;
            }
            JSONObject eattr = new JSONObject();
            eattr.put("wins", wins);
            eattr.put("matches", matches);
            edges.add(new Edge<>(players.get(player), heroes.get(hero), eattr));
        }

        JavaRDD<Tuple2<Object, JSONObject>> verticesRDD = sc.parallelize(vertices);
        JavaRDD<Edge<JSONObject>> edgesRDD = sc.parallelize(edges);

        Graph<JSONObject, JSONObject> graph = Graph.apply(verticesRDD.rdd(),
                edgesRDD.rdd(),
                null,
                StorageLevel.MEMORY_ONLY(),
                StorageLevel.MEMORY_ONLY(),
                ClassTag$.MODULE$.apply(JSONObject.class),
                ClassTag$.MODULE$.apply(JSONObject.class));

        this.graph = graph;
        this.players = players.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.heroes = heroes.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.graph = getPageRank(this.graph);
    }

    public Graph<JSONObject, JSONObject> getPageRank(Graph<JSONObject, JSONObject> graph) {
        Graph<Object, Object> graphWithPR = graph.ops().pageRank(0.0001, 0.15);
        Graph<JSONObject, JSONObject> newgraph = graph.outerJoinVertices(
                graphWithPR.vertices().toJavaRDD().rdd(),
                new SerializableFunction3<Object, JSONObject, Option<Object>, JSONObject>() {
                    @Override
                    public JSONObject apply(Object o, JSONObject jsonObject, Option<Object> objectOption) {
                        jsonObject.put("PR", (Double) objectOption.get());
                        return jsonObject;
                    }
                },
                ClassTag$.MODULE$.apply(JSONObject.class),
                ClassTag$.MODULE$.apply(JSONObject.class),
                Predef.$eq$colon$eq$.MODULE$.tpEquals()
        );
        return newgraph;
    }

    public Graph<JSONObject, JSONObject> getGraph(Map<String, Object> conditions) {
        String players = conditions.containsKey("players") ? (String) conditions.get("players") : "";
        List<String> playerArr = Arrays.asList(players.split(","));
        String heroes = conditions.containsKey("heroes") ? (String) conditions.get("heroes") : "";
        List<String> heroArr = Arrays.asList(heroes.split(","));
        Integer lowerWins = conditions.containsKey("lowerWins") ? (Integer) conditions.get("lowerWins") : 0;
        Integer lowerMatches = conditions.containsKey("lowerMatches") ? (Integer) conditions.get("lowerMatches") : 0;

        SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object> edgefilter =
                new SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object>() {
                    @Override
                    public Object apply(EdgeTriplet<JSONObject, JSONObject> etriplet) {
                        boolean pcond = false;
                        if (players.trim().length() == 0)
                            pcond = true;
                        else pcond = playerArr.contains(etriplet.srcAttr().getString("name"));

                        boolean hcond = false;
                        if (heroes.trim().length() == 0)
                            hcond = true;
                        else hcond = heroArr.contains(etriplet.dstAttr().getString("name"));

                        return etriplet.attr.getInteger("wins") >= lowerWins
                                && etriplet.attr.getInteger("matches") >= lowerMatches
                                && pcond && hcond;
                    }
                };

        List<Object> validVIDs = this.graph
                .triplets()
                .filter(edgefilter)
                .map(new SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object>() {
                    @Override
                    public Object apply(EdgeTriplet<JSONObject, JSONObject> edgeTriplet) {
                        return new Tuple2<>(edgeTriplet.srcId(), edgeTriplet.dstId());
                    }
                }, ClassTag$.MODULE$.apply(Tuple2.class)).toJavaRDD().collect();
        List<Long> ids = new ArrayList<>();
        validVIDs.forEach(o -> {
            ids.add((Long) ((Tuple2) o)._1);
            ids.add((Long) ((Tuple2) o)._2);
        });

        Graph<JSONObject, JSONObject> subgraph = this.graph.subgraph(
                edgefilter,
                new SerializableFunction2<Object, JSONObject, Object>() {
                    @Override
                    public Object apply(Object vid, JSONObject vattr) {
                        return ids.contains(vid);
                    }
                });
        return subgraph;
    }

    public JSONObject getDegrees(Map<String, Object> conditions) {
        Graph<JSONObject, JSONObject> subgraph = getGraph(conditions);
        List<Tuple2<Object, Object>> playerOutDegreesTuple = subgraph.ops().outDegrees().toJavaRDD().collect();
        List<JSONObject> playerOutDegrees = new ArrayList<>();
        playerOutDegreesTuple.forEach(t -> {
            JSONObject j = new JSONObject();
            j.put("id", t._1);
            j.put("name", players.get(t._1));
            j.put("type", "player");
            j.put("outDegrees", t._2);
            playerOutDegrees.add(j);
        });
        playerOutDegrees.sort(Comparator.comparingInt(o -> ((JSONObject) o).getInteger("outDegrees")).reversed());

        List<Tuple2<Object, Object>> heroInDegreesTuple = subgraph.ops().inDegrees().toJavaRDD().collect();
        List<JSONObject> heroInDegrees = new ArrayList<>();
        heroInDegreesTuple.forEach(t -> {
            JSONObject j = new JSONObject();
            j.put("id", t._1);
            j.put("name", heroes.get(t._1));
            j.put("type", "hero");
            j.put("inDegrees", t._2);
            heroInDegrees.add(j);
        });
        heroInDegrees.sort(Comparator.comparingInt(o -> ((JSONObject) o).getInteger("inDegrees")).reversed());

        JSONObject degrees = new JSONObject();
        int end = playerOutDegrees.size() > 10 ? 10 : playerOutDegrees.size();
        degrees.put("player", playerOutDegrees.subList(0, end));
        end = heroInDegrees.size() > 10 ? 10 : heroInDegrees.size();
        degrees.put("hero", heroInDegrees.subList(0, end));
        return degrees;
    }

    public JSONObject graph2JSON(Graph<JSONObject, JSONObject> graph) {
        List<JSONObject> vertices = graph.vertices().map(
                new SerializableFunction1<Tuple2<Object, JSONObject>, JSONObject>() {
                    @Override
                    public JSONObject apply(Tuple2<Object, JSONObject> v) {
                        JSONObject vjs = new JSONObject();
                        vjs.put("id", (Long) v._1);
                        vjs.put("name", v._2.getString("name"));
                        vjs.put("PR", v._2.getDouble("PR"));
                        vjs.put("type", v._2.getString("type"));
                        return vjs;
                    }
                }, ClassTag$.MODULE$.apply(JSONObject.class)).toJavaRDD().collect();
        List<JSONObject> edges = graph.triplets().map(
                new SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, JSONObject>() {
                    @Override
                    public JSONObject apply(EdgeTriplet e) {
                        JSONObject ejs = new JSONObject();
                        ejs.put("srcId", e.srcId());
                        ejs.put("dstId", e.dstId());
                        ejs.put("wins", ((JSONObject) e.attr).getInteger("wins"));
                        ejs.put("matches", ((JSONObject) e.attr).getInteger("matches"));
                        return ejs;
                    }
                }, ClassTag$.MODULE$.apply(JSONObject.class)).toJavaRDD().collect();

        JSONObject info = new JSONObject();
        info.put("vertices", vertices);
        info.put("edges", edges);
        return info;
    }

}
