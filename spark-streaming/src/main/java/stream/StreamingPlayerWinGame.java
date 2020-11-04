package stream;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kafka.KafkaSink;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function3;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import pojo.PlayerMessage;
import pojo.PlayerResult;
import scala.Tuple2;
import util.KafKaUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamingPlayerWinGame extends StreamJobBuilder {
    public StreamingPlayerWinGame(JavaStreamingContext jssc) {
        super(jssc);
    }

    public StreamingPlayerWinGame() {

    }

    @Override
    public void buildJob() {
        final JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        super.jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(
                                Collections.singletonList(KafKaUtil.SOURCE_TOPIC),
                                KafKaUtil.getConsumerParams("playerMessage")
                        )
                );

        JavaDStream<JSONObject> matches = stream.map(
                (Function<ConsumerRecord<String, String>, JSONObject>) mt -> JSONObject.parseObject(mt.value())
        );
        JavaDStream<List<Tuple2<String, PlayerMessage>>> playerMatch = matches.map(mat -> {
            List<Tuple2<String, PlayerMessage>> res = new ArrayList<>();
            String winId = mat.getString("winner_id");
            String playTime = mat.getJSONObject("race_info").getString("date_time");
            //无法判断比赛时间直接返回
            if (playTime == null || "".equals(playTime.trim())) return res;
            JSONArray array = winId.equals(mat.getJSONObject("red_team").getString("team_id")) ?
                    mat.getJSONObject("red_team").getJSONArray("players") :
                    mat.getJSONObject("blue_team").getJSONArray("players");
            for (int i = 0; i < array.size(); i++) {
                JSONObject player = array.getJSONObject(i);
                PlayerMessage message = new PlayerMessage();
                message.setPlayerId(player.getString("player_id"));
                message.setPlayerName(player.getString("player_name"));
                message.setMatchDate(playTime);
                res.add(new Tuple2<>(message.getPlayerId(), message));
            }
            return res;
        });
        JavaPairDStream<String, PlayerMessage> players = playerMatch.flatMap(t -> {
            return t.iterator();
        }).mapToPair(t -> t);

        StateSpec<String, PlayerMessage, PlayerResult, Tuple2<String, PlayerResult>> stateCum = StateSpec.function(
                (Function3<String, Optional<PlayerMessage>, State<PlayerResult>, Tuple2<String, PlayerResult>>)
                        (key, curOptional, state) -> {
                            PlayerResult result = state.exists() ? state.get() : new PlayerResult();
                            PlayerMessage cur = curOptional.orElse(null);
                            if (cur != null) {
                                result.setWinGames(result.getWinGames() + 1);
                                result.setPlayerId(cur.getPlayerId());
                                result.setPlayerName(cur.getPlayerName());

                                result.setUpdateTime(cur.getMatchDate());
                            }

                            state.update(result);

                            return Tuple2.apply(key, result);
                        }
        );

        JavaPairDStream<String, PlayerResult> playerResultJavaPairDStream = players.mapWithState(stateCum).stateSnapshots();
        playerResultJavaPairDStream.foreachRDD(
                rdd -> {
                    rdd.foreachPartition(it -> {
                        KafkaSink kafkaSink = KafkaSink.getInstance();
                        while (it.hasNext()) {
                            Tuple2<String, PlayerResult> entry = it.next();
                            JSONObject playWinGames = new JSONObject();
                            playWinGames.put("play_id", entry._1);
                            playWinGames.put("play_name", entry._2.getPlayerName());
                            playWinGames.put("winGames", entry._2.getWinGames());
                            playWinGames.put("updateTime", entry._2.getUpdateTime());
                            System.out.println(playWinGames);
                            kafkaSink.send(new ProducerRecord<>(KafKaUtil.PLAYER_WIN_TOPIC, playWinGames.toJSONString()));

                        }
                    });

                }
        );

    }

}
