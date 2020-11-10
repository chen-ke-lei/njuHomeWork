package stream;

import com.alibaba.fastjson.JSONObject;
import kafka.KafkaSink;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;
import util.KafKaUtil;

import java.util.Collections;
import java.util.Iterator;

public class StreamTeamWinRate extends StreamJobBuilder {

    public StreamTeamWinRate(JavaStreamingContext jssc) {
        super(jssc);
    }

    public StreamTeamWinRate() {}

    @Override
    public void buildJob() {
        // 获取kafka的数据，从SOURCE_TOPIC读取，设置jobName作为分组来重复消费
        final JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        super.jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(
                                Collections.singletonList(KafKaUtil.SOURCE_TOPIC),
                                KafKaUtil.getConsumerParams("teamWinRate")
                        )
                );

        JavaDStream<JSONObject> matches = stream.map(
                (Function<ConsumerRecord<String, String>, JSONObject>) mt -> JSONObject.parseObject(mt.value())
        );

        JavaPairDStream<String, Integer> winTeam = matches.mapToPair
                ((PairFunction<JSONObject, String, Integer>) mat -> {
                    String win = mat.getString("winner_id");
                    String team = "";
                    if (win.equals(mat.getJSONObject("blue_team").getString("team_id")))
                        team = mat.getJSONObject("blue_team").getString("team_name");
                    else team = mat.getJSONObject("red_team").getString("team_name");
                    return new Tuple2<>(team, 1);
                });
        JavaPairDStream<String, Integer> buleTeam = matches.mapToPair(
                (PairFunction<JSONObject, String, Integer>) mat -> {
                    String team = mat.getJSONObject("blue_team").getString("team_name");
                    return new Tuple2<>(team, 1);
                });
        JavaPairDStream<String, Integer> redTeam = matches.mapToPair(
                (PairFunction<JSONObject, String, Integer>) mat -> {
                    String team = mat.getJSONObject("red_team").getString("team_name");
                    return new Tuple2<>(team, 1);
                });
        JavaPairDStream<String, Integer> teams = buleTeam.union(redTeam);

        // reduce the batch first can reduce the records in the batch
//        winTeam = winTeam.reduceByKey(
//                (Function2<Integer, Integer, Integer>) Integer::sum
//        );
//        teams = teams.reduceByKey(
//                (Function2<Integer, Integer, Integer>) Integer::sum
//        );

        /*
          注意要采用State方法才能实现累加，不然仅会统计当前batch。
          下面我觉得都没需要，设置越多越容易出bug
          如果有初始化的值得需要，可以使用initialState(RDD)来初始化key的；
          可以指定timeout函数，该函数的作用是，如果一个key超过timeout设定的时间没有更新值，那么这个key将会失效。
          这个控制需要在Func中实现，必须使用state.isTimingOut()来判断失效的key值。
          如果在失效时间之后，这个key又有新的值了，则会重新计算。
          如果没有使用isTimingOut，则会报错。
         */
        StateSpec<String, Integer, Integer, Tuple2<String, Integer>> stateCum = StateSpec.function(
                // String 代表要更新的State对象Key
                // Optional<Integer> 代表本批次计算得到key对应的value值，可能没有，如第一批进入，所以是Optional。
                // State<Integer>当前Key的State，在State中保存的旧的value值，调用函数的时候已经赋值。在代码里可以实现创建更新等操作：可以累加；可以比较大小，更新一个更大值，等等。
                // Tuple2<String, Integer>是函数返回值，State的一个item。返回Tuple2就更新State中相应Key的数据，调用remove可以删除State中的Key对象。
                (Function3<String, Optional<Integer>, State<Integer>, Tuple2<String, Integer>>)
                        (key, curOptional, state) -> {
                            int prev = state.exists() ? state.get() : 0;
                            int cur = curOptional.orElse(0);
                            int sum = prev + cur;
                            if (state.exists()) {
                                state.update(sum);
                            } else {
                                state.update(cur);
                            }
                            return Tuple2.apply(key, sum);
                        }
        );
        JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> allWin = winTeam.mapWithState(stateCum);
        JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> allMatch = teams.mapWithState(stateCum);

        // This only includes states updated in the batch
        JavaPairDStream<String, Integer> teamWins = allWin.mapToPair(
                (PairFunction<Tuple2<String, Integer>, String, Integer>) data -> new Tuple2<>(data._1, data._2)
        );
        JavaPairDStream<String, Integer> teamMatches = allMatch.mapToPair(
                (PairFunction<Tuple2<String, Integer>, String, Integer>) data -> new Tuple2<>(data._1, data._2)
        );

        // This will include all states
//        JavaPairDStream<String, Integer> teamWins = allWin.stateSnapshots();
//        JavaPairDStream<String, Integer> teamMatches = allMatch.stateSnapshots();

        // Calculate the sum and display
        JavaDStream<Tuple2<String, Integer> > sum = teamWins.reduce(
                (Function2<Tuple2<String, Integer>, Tuple2<String, Integer>, Tuple2<String, Integer>>)
                        (one ,two) -> new Tuple2<>("sum", one._2 + two._2)
        );
        sum.print();

        JavaPairDStream<String, Tuple2<Integer, Integer>> joinRes = teamWins.join(teamMatches).filter(x -> x._2._2 > 10);
        JavaDStream<Tuple2<String, Double>> winRate = joinRes.map(
                (Function<Tuple2<String, Tuple2<Integer, Integer>>, Tuple2<String, Double>>) s ->
                        new Tuple2<>(s._1, s._2._1 / (s._2._2 * 1.0))
        );

//        winRate.foreachRDD((VoidFunction<JavaRDD<Tuple2<String, Double>>>) rdd -> {
//            rdd.sortBy((Function<Tuple2<String, Double>, Double>) t -> {
//                return t._2;
//            }, false, 2).take(5).stream().forEach(x -> System.out.println(x));
//            System.out.println("=================");
//        });
        winRate.foreachRDD(
                (VoidFunction<JavaRDD<Tuple2<String, Double>>>) rdd -> {
                    rdd.foreachPartition(
                            (VoidFunction<Iterator<Tuple2<String, Double>>>) entryIt -> {
                                KafkaSink kafkaSink = KafkaSink.getInstance();
                                while (entryIt.hasNext()) {
                                    Tuple2<String, Double> entry = entryIt.next();
                                    JSONObject record = new JSONObject();
                                    record.put("team_name", entry._1);
                                    record.put("win_rate", entry._2);
                                    // System.out.println(record);
                                    kafkaSink.send(new ProducerRecord<>("teamWinRate", record.toJSONString()));
                                }
                            }
                    );
                }
        );
    }
}
