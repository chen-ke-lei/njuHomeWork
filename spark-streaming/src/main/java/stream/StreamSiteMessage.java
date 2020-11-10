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
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import pojo.HeroInfo;
import pojo.HeroResult;
import pojo.SiteMessage;
import scala.Tuple2;
import util.KafKaUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamSiteMessage extends StreamJobBuilder {
    public StreamSiteMessage(JavaStreamingContext jssc) {
        super(jssc);
    }

    public StreamSiteMessage() {

    }

    @Override
    public void buildJob() {
        final JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        super.jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(
                                Collections.singletonList(KafKaUtil.SOURCE_TOPIC),
                                KafKaUtil.getConsumerParams("siteMessage")
                        )
                );

        JavaDStream<JSONObject> matches = stream.map(
                (Function<ConsumerRecord<String, String>, JSONObject>) mt -> JSONObject.parseObject(mt.value())
        );

        String[] siteMesage = new String[]{"TOP", "JUNGLE", "MIDDLE", "BOTTOM", "SUPPORT"};

        JavaDStream<List<Tuple2<String, SiteMessage>>> sitesByGame = matches.map(mat -> {
            List<Tuple2<String, SiteMessage>> res = new ArrayList<>();
            //    String winId = mat.getString("winner_id");
            String playTime = mat.getJSONObject("race_info").getString("date_time");
            //无法判断比赛时间直接返回
            if (playTime == null || "".equals(playTime.trim())) return res;
            JSONObject bule = mat.getJSONObject("blue_team");
            //  String buleID = bule.getString("team_id");
            for (int i = 0; i < bule.getJSONArray("players").size(); i++) {
                JSONObject stats = ((JSONObject) bule.getJSONArray("players").get(i)).getJSONObject("stats");
                if (stats == null || stats.get("totalHeal") == null) return res;
                SiteMessage message = new SiteMessage();
                message.setUpdateTime(playTime);
                message.setDamageDealt(new Double(stats.getString("totalDamageDealt").replaceAll("[^0-9\\.]", "")));
                message.setHeal(new Double(stats.getString("totalHeal").replaceAll("[^0-9\\.]", "")));
                message.setDamageTaken(new Double(stats.getString("totalDamageTaken").replaceAll("[^0-9\\.]", "")));
                message.calTotalPoint();
                String site = ((JSONObject) bule.getJSONArray("players").get(i)).getString("lane");
                if (site == null || site.trim().length() == 0) {
                    site = siteMesage[i];
                }
                message.setSite(site);
                res.add(new Tuple2<>(site, message));

            }
            if (res.size() < 5) return res;
            JSONObject red = mat.getJSONObject("red_team");
            for (int i = 0; i < red.getJSONArray("players").size(); i++) {
                JSONObject stats = ((JSONObject) red.getJSONArray("players").get(i)).getJSONObject("stats");
                if (stats == null || stats.get("totalHeal") == null) return res;
                SiteMessage message = null;
                String site = ((JSONObject) red.getJSONArray("players").get(i)).getString("lane");
                if (site == null || site.trim().length() == 0) {
                    site = siteMesage[i];
                }
                for (int j = 0; j < res.size(); j++)
                    if (res.get(j)._1.equals(site)) {
                        message = res.get(j)._2;
                        break;
                    }
                if (message == null) {
                    message = new SiteMessage();
                    message.setSite(site);
                    message.setUpdateTime(playTime);
                    res.add(new Tuple2<>(site, message));
                }
                message.setDamageDealt(message.getDamageDealt() + new Double(stats.getString("totalDamageDealt").replaceAll("[^0-9\\.]", "")));
                message.setHeal(message.getHeal() + new Double(stats.getString("totalHeal").replaceAll("[^0-9\\.]", "")));
                message.setDamageTaken(message.getDamageTaken() + new Double(stats.getString("totalDamageTaken").replaceAll("[^0-9\\.]", "")));
                message.calTotalPoint();

            }
            return res;
        });

        JavaPairDStream<String, SiteMessage> siteMessage = sitesByGame.flatMap(t -> {
            return t.iterator();
        }).mapToPair(t -> t);


        StateSpec<String, SiteMessage, SiteMessage, Tuple2<String, SiteMessage>> stateCum = StateSpec.function(
                (Function3<String, Optional<SiteMessage>, State<SiteMessage>, Tuple2<String, SiteMessage>>)
                        (key, curOptional, state) -> {
                            SiteMessage result = state.exists() ? state.get() : null;
                            SiteMessage cur = curOptional.orElse(null);
                            if (cur == null) {
                                return Tuple2.apply(key, result);
                            } else if (result == null) {
                                state.update(cur);
                                return Tuple2.apply(key, cur);
                            } else {
                                result.update(cur);
                                state.update(result);
                                return Tuple2.apply(key, result);
                            }


                        }
        );
        JavaPairDStream<String, SiteMessage> siteMessageJavaPairDStream = siteMessage.mapWithState(stateCum).stateSnapshots();
        siteMessageJavaPairDStream.foreachRDD(
                rdd -> {
                    rdd.foreachPartition(it -> {
                        KafkaSink kafkaSink = KafkaSink.getInstance();
                        while (it.hasNext()) {
                            Tuple2<String, SiteMessage> entry = it.next();
                            JSONObject siteJSON = new JSONObject();
                            siteJSON.put("site", entry._1);
                            siteJSON.put("heal", entry._2.getHeal());
                            siteJSON.put("damageTaken", entry._2.getDamageTaken());
                            siteJSON.put("damageDealt", entry._2.getDamageDealt());
                            siteJSON.put("totalPoint", entry._2.getTotalPoint());
                            siteJSON.put("updateTime", entry._2.getUpdateTime());
                            System.out.println(siteJSON);
                            kafkaSink.send(new ProducerRecord<>(KafKaUtil.SITE_MASSAGE_TOPIC, siteJSON.toJSONString()));
                        }
                    });

                }
        );


    }
}
