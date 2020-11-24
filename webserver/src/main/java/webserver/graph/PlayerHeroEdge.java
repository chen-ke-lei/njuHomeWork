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

public class PlayerHeroEdge {

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
        JavaRDD<List<Tuple2<String, JSONObject>>> playersRDD = jsonStrRDD.map(
                (Function<String, List<Tuple2<String, JSONObject>>>) jsonStr -> {
                    List<Tuple2<String, JSONObject>> res = new ArrayList<>();

                    JSONObject match = JSONObject.parseObject(jsonStr);
                    String winId = match.getString("winner_id");

                    String playTime = match.getJSONObject("race_info").getString("date_time");
                    if (playTime == null || "".equals(playTime.trim())) return res;

                    JSONObject blue = match.getJSONObject("blue_team");
                    String blueId = blue.getString("team_id");
                    String blueName = blue.getString("team_name");
                    for (int i = 0; i < blue.getJSONArray("players").size(); i++) {
                        JSONObject player = (JSONObject) blue.getJSONArray("players").get(i);
                        JSONObject rec = new JSONObject();
                        String playerName = player.getString("player_name").toLowerCase();
                        rec.put("playerId", player.getString("player_id"));
                        rec.put("playerName", playerName);
                        rec.put("heroId", player.getString("hero_id"));
                        rec.put("heroName", player.getString("hero_name"));
                        rec.put("teamId", blueId);
                        rec.put("teamName", blueName);
                        rec.put("win", winId.equals(blueId));
                        res.add(new Tuple2<>(playerName, rec));
                    }

                    JSONObject red = match.getJSONObject("red_team");
                    String redId = red.getString("team_id");
                    String redName = red.getString("team_name");
                    for (int i = 0; i < red.getJSONArray("players").size(); i++) {
                        JSONObject player = (JSONObject) red.getJSONArray("players").get(i);
                        JSONObject rec = new JSONObject();
                        String playerName = player.getString("player_name").toLowerCase();
                        rec.put("playerId", player.getString("player_id"));
                        rec.put("playerName", playerName);
                        rec.put("heroId", player.getString("hero_id"));
                        rec.put("heroName", player.getString("hero_name"));
                        rec.put("teamId", redId);
                        rec.put("teamName", redName);
                        rec.put("win", winId.equals(redId));
                        res.add(new Tuple2<>(playerName, rec));
                    }
                    return res;
                });

        JavaPairRDD<String, JSONObject> playerRDD = playersRDD.flatMap(List::iterator).mapToPair(t -> t);

        JavaPairRDD<Tuple2<String, String>, Integer> playerHeroMatch = playerRDD.mapToPair(
                (PairFunction<Tuple2<String, JSONObject>, Tuple2<String, String>, Integer>) data -> {
                    String playerName = data._1;
                    String heroName = data._2.getString("heroName");
                    return new Tuple2<>(new Tuple2<>(playerName, heroName), 1);
                }
        );
        JavaPairRDD<Tuple2<String, String>, Integer> playerHeroWin = playerRDD.mapToPair(
                (PairFunction<Tuple2<String, JSONObject>, Tuple2<String, String>, Integer>) data -> {
                    String playerName = data._1;
                    String heroName = data._2.getString("heroName");
                    boolean win = data._2.getBoolean("win");
                    return new Tuple2<>(new Tuple2<>(playerName, heroName), win ? 1 : 0);
                }
        );

        JavaPairRDD<Tuple2<String, String>, Integer> playerHeroMatchCount = playerHeroMatch.reduceByKey(Integer::sum);
        JavaPairRDD<Tuple2<String, String>, Integer> playerHeroWinCount = playerHeroWin.reduceByKey(Integer::sum);

        JavaPairRDD<Tuple2<String, String>, Tuple2<Integer, Integer>> playerHero = playerHeroWinCount.join(playerHeroMatchCount);

        List<Tuple2<Tuple2<String, String>, Tuple2<Integer, Integer>>> collected = playerHero.collect();

        List<JSONObject> res = new ArrayList<>();
        for (int i = 0; i < collected.size(); i++) {
            Tuple2<Tuple2<String, String>, Tuple2<Integer, Integer>> obj = collected.get(i);
            JSONObject formatted = new JSONObject();
            String player = obj._1._1;
            String hero = obj._1._2;
            Integer wins = obj._2._1;
            Integer matches = obj._2._2;
            if (player.length() == 0 || hero.length() == 0) continue;
            formatted.put("player", player);
            formatted.put("hero", hero);
            formatted.put("wins", wins);
            formatted.put("matches", matches);
            res.add(formatted);
        }
        FileUtil.writeFile(JSONObject.toJSONString(res), "/Users/Trayvon/Desktop/player_hero_edge.json");
    }
}
