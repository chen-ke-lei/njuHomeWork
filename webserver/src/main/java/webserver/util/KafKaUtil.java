package webserver.util;

import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.*;

public class KafKaUtil {

    public static List<String> TOPICS = new ArrayList<>(
            Arrays.asList("heroMatches", "heroWinRate", "playerWin", "siteMessage", "teamWinRate")
    );

    public static String BOOTSTRAP_SERVERS = "localhost:9092";

    public static boolean isValidTopic(String topic) {
        return TOPICS.contains(topic);
    }

    public static Map<String, Object> getConsumerParams(String groupId) {
        Map<String, Object> kafkaParams = new HashMap<>();
        // Kafka服务监听端口
        kafkaParams.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        // 指定kafka输出key的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        // 指定kafka输出value的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        // 消费者组ID
        kafkaParams.put("group.id", groupId);
        // 指定从earliest(若有无提交offset,则重新开始)/latest(若有无提交offset,则从新产生开始)/none(报异常)
        kafkaParams.put("auto.offset.reset", "latest");
        // 如果true,consumer定期地往zookeeper写入每个分区的offset
        kafkaParams.put("enable.auto.commit", false);

        return kafkaParams;
    }

}
