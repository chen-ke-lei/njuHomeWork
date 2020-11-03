package kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import util.KafKaUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 此类是为了将spark streaming的结果输出至Kafka而设计的可序列化Producer
 * 默认Topic为String，消息为Json字符串，原因是泛型单例我不会写
 */
public class KafkaSink implements Serializable {

    private static KafkaSink kafkaSink = null;

    private KafkaProducer<String, String> kafkaProducer;

    private KafkaSink() {
        Map<String, Object> kafkaParams = KafKaUtil.getProducerParams();
        this.kafkaProducer = new KafkaProducer<>(kafkaParams);
    }

    public static synchronized KafkaSink getInstance() {
        if (kafkaSink == null) {
            kafkaSink = new KafkaSink();
        }
        return kafkaSink;
    }

    public void send(ProducerRecord<String, String> record) {
        kafkaProducer.send(record);
    }

    public void shutdown() {
        kafkaProducer.close();
    }

}