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
import scala.Tuple2;
import scala.reflect.ClassTag$;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import webserver.util.FileUtil;

import java.io.Serializable;
import java.util.*;

@Component
public class PlayerHeroGraph implements Serializable {
    static abstract class SerializableFunction1<T1, R> extends AbstractFunction1<T1, R> implements Serializable {
    }

    static abstract class SerializableFunction2<T1, T2, R> extends AbstractFunction2<T1, T2, R> implements Serializable {
    }

    @Autowired
    private transient JavaSparkContext sc;

    private static Integer lowestMatches = 35; // 限制35场比赛以上，不然前端画不动

    private Graph<Tuple2<String, String>, Tuple2<Integer, Integer>> graph;

    public void buildGraph() {
        List<Tuple2<Object, Tuple2<String, String>>> vertices = new ArrayList<>();
        List<Edge<Tuple2<Integer, Integer>>> edges = new ArrayList<>();
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
            if (matches < lowestMatches) continue;
            if (!players.containsKey(player)) {
                players.put(player, id);
                vertices.add(new Tuple2<>(id, new Tuple2<>(player, "player")));
                id++;
            }
            if (!heroes.containsKey(hero)) {
                heroes.put(hero, id);
                vertices.add(new Tuple2<>(id, new Tuple2<>(hero, "hero")));
                id++;
            }
            edges.add(new Edge<>(players.get(player), heroes.get(hero), new Tuple2<>(wins, matches)));
        }

        JavaRDD<Tuple2<Object, Tuple2<String, String>>> verticesRDD = sc.parallelize(vertices);
        JavaRDD<Edge<Tuple2<Integer, Integer>>> edgesRDD = sc.parallelize(edges);

        Graph<Tuple2<String, String>, Tuple2<Integer, Integer>> graph = Graph.apply(verticesRDD.rdd(),
                edgesRDD.rdd(),
                null,
                StorageLevel.MEMORY_ONLY(),
                StorageLevel.MEMORY_ONLY(),
                ClassTag$.MODULE$.apply(Tuple2.class),
                ClassTag$.MODULE$.apply(Tuple2.class));
        this.graph = graph;
    }

    public Graph<Tuple2<String, String>, Tuple2<Integer, Integer>> getGraph(Map<String, Object> conditions) {
        String players = conditions.containsKey("players") ? (String) conditions.get("players") : "";
        List<String> playerArr = Arrays.asList(players.split(","));
        String heroes = conditions.containsKey("heroes") ? (String) conditions.get("heroes") : "";
        List<String> heroArr = Arrays.asList(heroes.split(","));
        Integer lowerWins = conditions.containsKey("lowerWins") ? (Integer) conditions.get("lowerWins") : 0;
        Integer lowerMatches = conditions.containsKey("lowerMatches") ? (Integer) conditions.get("lowerMatches") : lowestMatches;

        List<Object> validVIDs = this.graph
                .triplets()
                .filter(
                        new SerializableFunction1<EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>>, Object>() {
                            @Override
                            public Object apply(EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>> etriplet) {
                                boolean pcond = false;
                                if (players.trim().length() == 0)
                                    pcond = true;
                                else pcond = playerArr.contains(etriplet.srcAttr()._1);

                                boolean hcond = false;
                                if (heroes.trim().length() == 0)
                                    hcond = true;
                                else hcond = heroArr.contains(etriplet.dstAttr()._1);

                                return etriplet.attr._1 >= lowerWins && etriplet.attr._2 >= lowerMatches && pcond && hcond;
                            }
                        })
                .map(
                        new SerializableFunction1<EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>>, Object>() {
                            @Override
                            public Object apply(EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>> edgeTriplet) {
                                return new Tuple2<>(edgeTriplet.srcId(), edgeTriplet.dstId());
                            }
                        }, ClassTag$.MODULE$.apply(Tuple2.class)).toJavaRDD().collect();
        List<Long> ids = new ArrayList<>();
        validVIDs.forEach(o -> {
            ids.add((Long) ((Tuple2) o)._1);
            ids.add((Long) ((Tuple2) o)._2);
        });

        Graph<Tuple2<String, String>, Tuple2<Integer, Integer>> subgraph = this.graph.subgraph(
                new SerializableFunction1<EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>>, Object>() {
                    @Override
                    public Object apply(EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>> etriplet) {
                        return true;
                    }
                },
                new SerializableFunction2<Object, Tuple2<String, String>, Object>() {
                    @Override
                    public Object apply(Object vid, Tuple2<String, String> vattr) {
                        return ids.contains(vid);
                    }
                });
        return subgraph;
    }

    public JSONObject toJSON(Graph<Tuple2<String, String>, Tuple2<Integer, Integer>> graph) {
        List<JSONObject> vertices = graph.vertices().map(new SerializableFunction1<Tuple2<Object, Tuple2<String, String>>, JSONObject>() {
            @Override
            public JSONObject apply(Tuple2<Object, Tuple2<String, String>> v) {
                JSONObject vjs = new JSONObject();
                vjs.put("id", (Long) v._1);
                vjs.put("name", v._2._1);
                vjs.put("type", v._2._2);
                return vjs;
            }
        }, ClassTag$.MODULE$.apply(JSONObject.class)).toJavaRDD().collect();
        List<JSONObject> edges = graph.triplets().map(new SerializableFunction1<EdgeTriplet<Tuple2<String, String>, Tuple2<Integer, Integer>>, JSONObject>() {
            @Override
            public JSONObject apply(EdgeTriplet e) {
                JSONObject ejs = new JSONObject();
                ejs.put("srcId", e.srcId());
                ejs.put("dstId", e.dstId());
                ejs.put("wins", ((Tuple2) e.attr)._1);
                ejs.put("matches", ((Tuple2) e.attr)._2);
                return ejs;
            }
        }, ClassTag$.MODULE$.apply(JSONObject.class)).toJavaRDD().collect();

        JSONObject info = new JSONObject();
        info.put("vertices", vertices);
        info.put("edges", edges);
        return info;
    }

}
