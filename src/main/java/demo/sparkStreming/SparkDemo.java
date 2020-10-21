package demo.sparkStreming;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.dstream.DStream;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.stringtemplate.v4.ST;
import scala.Int;
import scala.Tuple2;
import util.KafKaUtil;

import java.util.*;

public class SparkDemo {
    public static void main(String[] args) {
        // 构建SparkStreaming上下文
        System.setProperty("hadoop.home.dir", "C:\\bigdata\\hadoop-2.7.1");
        SparkConf conf = new SparkConf().setAppName("Demo").setMaster("local");
        conf.setSparkHome("C:\\bigdata\\spark-2.4.7-bin-hadoop2.7\\");
        // 每隔5秒钟，sparkStreaming作业就会收集最近5秒内的数据源接收过来的数据
//        JavaSparkContext javaSparkContext=new JavaSparkContext(conf);
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));
        jssc.sparkContext().setLogLevel("ERROR");
        //checkpoint目录
        //jssc.checkpoint(ConfigurationManager.getProperty(Constants.STREAMING_CHECKPOINT_DIR));
        jssc.checkpoint("/streaming_checkpoint");

        // 构建kafka参数map
        // 主要要放置的是连接的kafka集群的地址（broker集群的地址列表）
        Map<String, Object> kafkaParams = new HashMap<String, Object>();
        //Kafka服务监听端口
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        //指定kafka输出key的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        //指定kafka输出value的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        //消费者ID，随意指定
        kafkaParams.put("group.id", "test_01");
        //指定从latest(最新,其他版本的是largest这里不行)还是smallest(最早)处开始读取数据
        kafkaParams.put("auto.offset.reset", "latest");
        //如果true,consumer定期地往zookeeper写入每个分区的offset
        kafkaParams.put("enable.auto.commit", false);


        // 构建topic set
        String kafkaTopics = KafKaUtil.topic;
        String[] kafkaTopicsSplited = kafkaTopics.split(",");

        Set<String> topics = new HashSet<String>();
        for (String kafkaTopic : kafkaTopicsSplited) {
            topics.add(kafkaTopic);
        }


        try {
            // 获取kafka的数据
            final JavaInputDStream<ConsumerRecord<String, String>> stream =
                    KafkaUtils.createDirectStream(
                            jssc,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
                    );

            JavaDStream<JSONObject> matches = stream.map((Function<ConsumerRecord<String, String>, JSONObject>) mt -> {
                JSONObject matchJson = JSONObject.parseObject(mt.value());
                return matchJson;
            });
            JavaPairDStream<String, Integer> winMatch = matches.mapToPair((PairFunction<JSONObject, String, Integer>) mat -> {
                String win = mat.getString("winner_id");
                String team = "";
                if (win.equals(mat.getJSONObject("blue_team").getString("team_id")))
                    team = mat.getJSONObject("blue_team").getString("team_name");
                else team = mat.getJSONObject("red_team").getString("team_name");
                return new Tuple2<>(team, 1);
            });
            JavaPairDStream<String, Integer> buleTeam = matches.mapToPair((PairFunction<JSONObject, String, Integer>) mat -> {
                String team = mat.getJSONObject("blue_team").getString("team_name");
                return new Tuple2<>(team, 1);
            });

            JavaPairDStream<String, Integer> redTeam = matches.mapToPair((PairFunction<JSONObject, String, Integer>) mat -> {
                String team = mat.getJSONObject("red_team").getString("team_name");
                return new Tuple2<>(team, 1);
            });

            JavaPairDStream<String, Integer> teams = buleTeam.union(redTeam);

            JavaPairDStream<String, Integer> allWin = winMatch.reduceByKeyAndWindow((Function2<Integer, Integer, Integer>) (a, b) -> {
                return a + b;
            }, Durations.seconds(15), Durations.seconds(5));
            JavaPairDStream<String, Integer> allTeam = teams.reduceByKeyAndWindow((Function2<Integer, Integer, Integer>) (a, b) -> {
                return a + b;
            }, Durations.seconds(15), Durations.seconds(5));
//            allWin.print();
//            allTeam.print();
            JavaPairDStream<String, Tuple2<Integer, Integer>> joinRes = allWin.join(allTeam).filter(x -> x._2._2 > 10);
            //    joinRes.print();
            JavaDStream<Tuple2<String, Double>> winRate = joinRes.map((Function<Tuple2<String, Tuple2<Integer, Integer>>, Tuple2<String, Double>>) s -> {
                return new Tuple2<String, Double>(s._1, s._2._1 / (s._2._2 * 1.0));
            });

            //         winRate.print();
            winRate.foreachRDD((VoidFunction<JavaRDD<Tuple2<String, Double>>>) rdd -> {
                rdd.sortBy((Function<Tuple2<String, Double>, Double>) t -> {
                    return t._2;
                }, false, 2).take(5).stream().forEach(x -> System.out.println(x));
                System.out.println("=================");
            });

            jssc.start();
            jssc.awaitTermination();
            jssc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
