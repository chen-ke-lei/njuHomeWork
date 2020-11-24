package webserver.graph;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;
import webserver.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class PlayerPlayerEdge {

    private static List<String> getMatchFiles() {
        List<String> files = new ArrayList<>();
        String date2matchJsonStr = FileUtil.readFile(FileUtil.INDEX_PATH);
        JSONObject date2matchJson = JSONObject.parseObject(date2matchJsonStr, Feature.OrderedField);

        for (String dateStr : date2matchJson.keySet()) {
            if (dateStr.equals("unkn") || dateStr.equals("unknown")) continue;
            JSONObject racesJson = date2matchJson.getJSONObject(dateStr);
            for (String raceId : racesJson.keySet()) {
                JSONArray matchesJson = racesJson.getJSONArray(raceId);
                for (String matchId : matchesJson.toJavaList(String.class)) {
                    String filePath = FileUtil.getMatchPath(dateStr, matchId);
                    files.add(filePath);
                }
            }
        }
        return files;
    }

    public static void main(String[] args) {
        SparkConf conf = new SparkConf().setAppName("LOLAnalGraph").setMaster("local[4]");
        JavaSparkContext sc = new JavaSparkContext(conf);

        List<String> matchFiles = getMatchFiles();
        JavaRDD<String> jsonStrRDD = sc.textFile(String.join(",", matchFiles));
        JavaRDD<List<JSONObject>> relationsRDD = jsonStrRDD.map(
                (Function<String, List<JSONObject>>) jsonStr -> {
                    List<JSONObject> res = new ArrayList<>();

                    JSONObject match = JSONObject.parseObject(jsonStr);
                    String winId = match.getString("winner_id");

                    String playTime = match.getJSONObject("race_info").getString("date_time");
                    if (playTime == null || "".equals(playTime.trim())) return res;

                    JSONArray blue = match.getJSONObject("blue_team").getJSONArray("players");
                    String blueId = match.getJSONObject("blue_team").getString("team_id");
                    for (int i = 0; i < blue.size() - 1; i++) {
                        for (int j = i + 1; j < blue.size(); j++) {
                            JSONObject playerOne = blue.getJSONObject(i);
                            JSONObject playerTwo = blue.getJSONObject(j);
                            JSONObject rec = new JSONObject();
                            rec.put("playerOne", playerOne.getString("player_name").toLowerCase());
                            rec.put("playerTwo", playerTwo.getString("player_name").toLowerCase());
                            rec.put("win", winId.equals(blueId));
                            res.add(rec);
                        }
                    }

                    JSONArray red = match.getJSONObject("red_team").getJSONArray("players");
                    String redId = match.getJSONObject("red_team").getString("team_id");
                    for (int i = 0; i < red.size() - 1; i++) {
                        for (int j = i + 1; j < red.size(); j++) {
                            JSONObject playerOne = red.getJSONObject(i);
                            JSONObject playerTwo = red.getJSONObject(j);
                            JSONObject rec = new JSONObject();
                            rec.put("playerOne", playerOne.getString("player_name").toLowerCase());
                            rec.put("playerTwo", playerTwo.getString("player_name").toLowerCase());
                            rec.put("win", winId.equals(redId));
                            res.add(rec);
                        }
                    }
                    return res;
                });

        JavaRDD<JSONObject> reRDD = relationsRDD.flatMap(List::iterator).map(t -> t);

        JavaPairRDD<Tuple2<String, String>, Integer> playerMateMatch = reRDD.mapToPair(
                (PairFunction<JSONObject, Tuple2<String, String>, Integer>) data -> {
                    String playerOne = data.getString("playerOne");
                    String playerTwo = data.getString("playerTwo");
                    return new Tuple2<>(new Tuple2<>(playerOne, playerTwo), 1);
                }
        );
        JavaPairRDD<Tuple2<String, String>, Integer> playerMateWin = reRDD.mapToPair(
                (PairFunction<JSONObject, Tuple2<String, String>, Integer>) data -> {
                    String playerOne = data.getString("playerOne");
                    String playerTwo = data.getString("playerTwo");
                    boolean win = data.getBoolean("win");
                    return new Tuple2<>(new Tuple2<>(playerOne, playerTwo), win ? 1 : 0);
                }
        );
        JavaPairRDD<Tuple2<String, String>, Integer> playerMateMatchCount = playerMateMatch.reduceByKey(Integer::sum);
        JavaPairRDD<Tuple2<String, String>, Integer> playerMateWinCount = playerMateWin.reduceByKey(Integer::sum);

        JavaPairRDD<Tuple2<String, String>, Tuple2<Integer, Integer>> playerHero = playerMateWinCount.join(playerMateMatchCount);

        List<Tuple2<Tuple2<String, String>, Tuple2<Integer, Integer>>> collected = playerHero.collect();

        List<JSONObject> res = new ArrayList<>();
        for (int i = 0; i < collected.size(); i++) {
            Tuple2<Tuple2<String, String>, Tuple2<Integer, Integer>> obj = collected.get(i);
            JSONObject no = new JSONObject();
            String playerOne = obj._1._1;
            String playerTwo = obj._1._2;
            Integer wins = obj._2._1;
            Integer matches = obj._2._2;
            if (playerOne.length() == 0 || playerTwo.length() == 0) continue;
            no.put("playerOne", playerOne);
            no.put("playerTwo", playerTwo);
            no.put("wins", wins);
            no.put("matches", matches);
            res.add(no);
        }
        FileUtil.writeFile(JSONObject.toJSONString(res), "/Users/Trayvon/Desktop/player_player_edge.json");
    }
}
