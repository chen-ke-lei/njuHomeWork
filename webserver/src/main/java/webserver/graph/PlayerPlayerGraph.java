package webserver.graph;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.EdgeTriplet;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.VertexRDD;
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
public class PlayerPlayerGraph implements Serializable {
    static abstract class SerializableFunction1<T1, R> extends AbstractFunction1<T1, R> implements Serializable {
    }

    static abstract class SerializableFunction2<T1, T2, R> extends AbstractFunction2<T1, T2, R> implements Serializable {
    }

    static abstract class SerializableFunction3<T1, T2, T3, R> extends AbstractFunction3<T1, T2, T3, R> implements Serializable {
    }

    @Autowired
    private transient JavaSparkContext sc;

    public static Integer lowestMatches = 200; // 限制200场比赛以上，不然前端画不动

    private Map<Long, String> players;

    private Graph<JSONObject, JSONObject> graph;

    public void buildGraph() {
        List<Tuple2<Object, JSONObject>> vertices = new ArrayList<>();
        List<Edge<JSONObject>> edges = new ArrayList<>();
        Map<String, Long> players = new HashMap<>();
        JSONArray jsonEdges = JSON.parseArray(FileUtil.readFile(FileUtil.PLAYER_PLAYER_EDGE_PATH));
        long id = 1L;
        for (int i = 0; i < jsonEdges.size(); i++) {
            JSONObject edge = jsonEdges.getJSONObject(i);
            String playerOne = edge.getString("playerOne");
            String playerTwo = edge.getString("playerTwo");
            Integer wins = edge.getInteger("wins");
            Integer matches = edge.getInteger("matches");
            if (matches < lowestMatches) continue;
            if (!players.containsKey(playerOne)) {
                JSONObject vattr = new JSONObject();
                vattr.put("name", playerOne);
                vattr.put("community", -1L);
                players.put(playerOne, id);
                vertices.add(new Tuple2<>(id, vattr));
                id++;
            }
            if (!players.containsKey(playerTwo)) {
                JSONObject vattr = new JSONObject();
                vattr.put("name", playerTwo);
                vattr.put("community", -1L);
                players.put(playerTwo, id);
                vertices.add(new Tuple2<>(id, vattr));
                id++;
            }
            JSONObject eattr = new JSONObject();
            eattr.put("wins", wins);
            eattr.put("matches", matches);
            edges.add(new Edge<>(players.get(playerTwo), players.get(playerOne), eattr));
            edges.add(new Edge<>(players.get(playerOne), players.get(playerTwo), eattr));
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
        this.graph = this.getStronglyConnectedComponents(this.graph);
        System.out.println("Player Player Graph" + "\r\n"
                + "vertices: " + vertices.size() + "\r\n"
                + "edges: " + edges.size());
    }

    public Graph<JSONObject, JSONObject> getGraph(Map<String, Object> conditions) {
        String players = conditions.containsKey("players") ? (String) conditions.get("players") : "";
        List<String> playerArr = Arrays.asList(players.split(","));
        Integer lowerWins = conditions.containsKey("lowerWins") ? (Integer) conditions.get("lowerWins") : 0;
        Integer lowerMatches = conditions.containsKey("lowerMatches") ? (Integer) conditions.get("lowerMatches") : 0;

        SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object> edgefilter =
                new SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object>() {
                    @Override
                    public Object apply(EdgeTriplet<JSONObject, JSONObject> etriplet) {
                        boolean pcond = false;
                        if (players.trim().length() == 0)
                            pcond = true;
                        else pcond =
                                playerArr.contains(etriplet.srcAttr().getString("name"))
                                        ||
                                        playerArr.contains(etriplet.dstAttr().getString("name"));

                        return etriplet.attr.getInteger("wins") >= lowerWins
                                && etriplet.attr.getInteger("matches") >= lowerMatches
                                && pcond;
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

    public List<JSONObject> getDegrees(Map<String, Object> conditions) {
        Graph<JSONObject, JSONObject> subgraph = getGraph(conditions);
        List<Tuple2<Object, Object>> playerOutDegreesTuple = subgraph.ops().degrees().toJavaRDD().collect();
        List<JSONObject> playerDegrees = new ArrayList<>();
        playerOutDegreesTuple.forEach(t -> {
            JSONObject j = new JSONObject();
            j.put("id", t._1);
            j.put("name", players.get(t._1));
            j.put("degrees", t._2);
            playerDegrees.add(j);
        });
        playerDegrees.sort(Comparator.comparingInt(o -> ((JSONObject) o).getInteger("degrees")).reversed());

        int end = playerDegrees.size() > 10 ? 10 : playerDegrees.size();
        return playerDegrees.subList(0, end);
    }

    public List<JSONObject> getTriangleCount (Map<String, Object> conditions) {
        Graph<JSONObject, JSONObject> subgraph = getGraph(conditions);
        List<Tuple2<Object, Object>> triangleTuple = subgraph.ops().triangleCount().vertices().toJavaRDD().collect();
        List<JSONObject> triangleCount = new ArrayList<>();
        triangleTuple.forEach(t -> {
            JSONObject j = new JSONObject();
            j.put("id", t._1);
            j.put("name", players.get(t._1));
            j.put("count", t._2);
            triangleCount.add(j);
        });
        triangleCount.sort(Comparator.comparingInt(o -> ((JSONObject) o).getInteger("count")).reversed());

        int end = triangleCount.size() > 10 ? 10 : triangleCount.size();
        return triangleCount.subList(0, end);
    }

    public Graph<JSONObject, JSONObject> getStronglyConnectedComponents(Graph<JSONObject, JSONObject> graph) {
        VertexRDD<Object> scc = graph.ops().stronglyConnectedComponents(5).vertices();

        Graph<JSONObject, JSONObject> graphWithCommunity = graph.outerJoinVertices(
                scc.toJavaRDD().rdd(),
                new SerializableFunction3<Object, JSONObject, Option<Object>, JSONObject>() {
                    @Override
                    public JSONObject apply(Object o, JSONObject jsonObject, Option<Object> leastVid) {
                        jsonObject.put("community", leastVid.get());
                        return jsonObject;
                    }
                },
                ClassTag$.MODULE$.apply(JSONObject.class),
                ClassTag$.MODULE$.apply(JSONObject.class),
                Predef.$eq$colon$eq$.MODULE$.tpEquals()
        );

        JavaPairRDD<Object, Object> validCommunity =
                scc.toJavaRDD()
                        .mapToPair((PairFunction<Tuple2<Object, Object>, Object, Object>) t -> new Tuple2<>(t._2, 1))
                        .reduceByKey((x, y) -> (int) x + (int) y)
                        .filter(t -> (int) t._2 > 1);
        JavaPairRDD<Object, Object> validMembers =
                scc.toJavaRDD()
                // leastIdInCommunity, memberId
                .mapToPair((PairFunction<Tuple2<Object, Object>, Object, Object>) t -> new Tuple2<>(t._2, t._1))
                // leastIdInCommunity, (memberId, Optional(size))
                .leftOuterJoin(validCommunity)
                .filter(t -> t._2._2.isPresent())
                // memberId, leastIdInCommunity
                .mapToPair((PairFunction<Tuple2<Object, Tuple2<Object, Optional<Object>>>, Object, Object>) t -> new Tuple2<>(t._2._1, t._1));
        List<Tuple2<Object, Object>> validIds = validMembers.collect();
        List<Long> ids = new ArrayList<>();
        validIds.forEach(t -> {
            ids.add((long) t._1);
            ids.add((long) t._2);
        });

        Graph<JSONObject, JSONObject> newgraph = graphWithCommunity.subgraph(
                new SerializableFunction1<EdgeTriplet<JSONObject, JSONObject>, Object>() {
                    @Override
                    public Object apply(EdgeTriplet<JSONObject, JSONObject> etriplet) {
                        long srcCommunity = etriplet.srcAttr().getLongValue("community");
                        long dstCommunity = etriplet.dstAttr().getLongValue("community");
                        return srcCommunity != -1L && dstCommunity != -1L && srcCommunity == dstCommunity;
                    }
                },
                new SerializableFunction2<Object, JSONObject, Object>() {
                    @Override
                    public Object apply(Object vid, JSONObject vattr) {
                        long communityId = vattr.getLongValue("community");
                        return communityId != -1L && ids.contains(communityId);
                    }
                }
        );
        return newgraph;
    }

    public JSONObject graph2JSON(Graph<JSONObject, JSONObject> graph) {
        List<JSONObject> vertices = graph.vertices().map(
                new SerializableFunction1<Tuple2<Object, JSONObject>, JSONObject>() {
                    @Override
                    public JSONObject apply(Tuple2<Object, JSONObject> v) {
                        JSONObject vjs = new JSONObject();
                        vjs.put("id", (Long) v._1);
                        vjs.put("name", v._2.getString("name"));
                        vjs.put("community", v._2.getLongValue("community"));
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
