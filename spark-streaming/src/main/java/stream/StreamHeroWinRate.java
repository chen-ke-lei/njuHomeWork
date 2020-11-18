package stream;

import com.alibaba.fastjson.JSONObject;
import kafka.KafkaSink;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function3;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import pojo.HeroInfo;
import pojo.HeroResult;
import scala.Tuple2;
import util.KafKaUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamHeroWinRate extends StreamJobBuilder implements Serializable {
    public StreamHeroWinRate(JavaStreamingContext jssc) {
        super(jssc);
    }

    public StreamHeroWinRate() {
    }

    @Override
    public void buildJob() {
        final JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        super.jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(
                                Collections.singletonList(KafKaUtil.SOURCE_TOPIC + "-" + KafKaUtil.HERO_WIN_RATE_TOPIC),
                                KafKaUtil.getConsumerParams("heroMessage")
                        )
                );

        JavaDStream<JSONObject> matches = stream.map(
                (Function<ConsumerRecord<String, String>, JSONObject>) mt -> JSONObject.parseObject(mt.value())
        );
        JavaDStream<List<Tuple2<String, HeroInfo>>> heros = matches.map(mat -> {
            List<Tuple2<String, HeroInfo>> res = new ArrayList<>();
            String winId = mat.getString("winner_id");
            String playTime = mat.getJSONObject("race_info").getString("date_time");
            //无法判断比赛时间直接返回
            if (playTime == null || "".equals(playTime.trim())) return res;
            JSONObject bule = mat.getJSONObject("blue_team");
            String buleID = bule.getString("team_id");
            for (int i = 0; i < bule.getJSONArray("players").size(); i++) {
                JSONObject player = (JSONObject) bule.getJSONArray("players").get(i);
                String heroId = player.getString("hero_id");
                res.add(new Tuple2<>(heroId, createHeroInfo(player, winId.equals(buleID), playTime)));
            }

            JSONObject red = mat.getJSONObject("red_team");
            String redId = red.getString("team_id");
            for (int i = 0; i < red.getJSONArray("players").size(); i++) {
                JSONObject player = (JSONObject) red.getJSONArray("players").get(i);
                String heroId = player.getString("hero_id");
                res.add(new Tuple2<>(heroId, createHeroInfo(player, winId.equals(redId), playTime)));
            }
            return res;
        });

        JavaPairDStream<String, HeroInfo> hero = heros.flatMap(t -> {
            return t.iterator();
        }).mapToPair(t -> t);


        StateSpec<String, HeroInfo, HeroResult, Tuple2<String, HeroResult>> stateCum = StateSpec.function(
                (Function3<String, Optional<HeroInfo>, State<HeroResult>, Tuple2<String, HeroResult>>)
                        (key, curOptional, state) -> {
                            HeroResult result = state.exists() ? state.get() : new HeroResult();
                            HeroInfo cur = curOptional.orElse(null);
                            if (cur != null) {
                                if ("99999999".equals(cur.getPlayTime())) {
                                    state.update(result);
                                    return Tuple2.apply(key, new HeroResult());
                                }
                                result.setPlayNum(result.getPlayNum() + 1);
                                result.setId(cur.getId());
                                result.setName(cur.getName());
                                if (cur.isWin()) {
                                    result.setWinNum(result.getWinNum() + 1);
                                    result.setWinRate(result.getWinNum() * 1.0 / result.getPlayNum());
                                }
                                result.setUpdateTime(cur.getPlayTime());
                            }

                            state.update(result);

                            return Tuple2.apply(key, result);
                        }
        );
        JavaMapWithStateDStream<String, HeroInfo, HeroResult, Tuple2<String, HeroResult>> heroStateDStream = hero.mapWithState(stateCum);
        //JavaPairDStream<String, HeroResult> heroResultJavaPairDStream = heroStateDStream.stateSnapshots();
        heroStateDStream.foreachRDD(
                rdd -> {
                    rdd.foreachPartition(it -> {
                        KafkaSink kafkaSink = KafkaSink.getInstance();
                        while (it.hasNext()) {
                            Tuple2<String, HeroResult> entry = it.next();
                            JSONObject playNum = new JSONObject();
//                            playNum.put("hero_id", entry._1);
//                            playNum.put("hero", entry._2.getName());
//                            playNum.put("playNum", entry._2.getPlayNum());
//                            playNum.put("updateTime", entry._2.getUpdateTime());
//                            System.out.println(playNum);
//                            kafkaSink.send(new ProducerRecord<>(KafKaUtil.HERO_MATCHES_TOPIC, playNum.toJSONString()));
                            JSONObject winRate = new JSONObject();
                            winRate.put("hero_id", entry._1);
                            winRate.put("hero", entry._2.getName());
                            winRate.put("winRate", entry._2.getWinRate());
                            winRate.put("updateTime", entry._2.getUpdateTime());
                            System.out.println(winRate);
                            kafkaSink.send(new ProducerRecord<>(KafKaUtil.HERO_WIN_RATE_TOPIC, winRate.toJSONString()));
                        }
                    });

                }
        );

    }

    private HeroInfo createHeroInfo(JSONObject player, boolean win, String dateTime) {
        String heroId = player.getString("hero_id");
        HeroInfo info = new HeroInfo();
        info.setId(heroId);
        info.setName(player.getString("hero_name"));
        info.setPlayTime(dateTime);
        info.setWin(win);
        return info;
    }
}

