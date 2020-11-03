package util;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

public class KafKaUtil {

    public static String SOURCE_TOPIC = "source";

    public static Map<String, Object> getConsumerParams(String jobName) {
        Map<String, Object> kafkaParams = new HashMap<>();
        // Kafka服务监听端口
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        // 指定kafka输出key的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        // 指定kafka输出value的数据类型及编码格式（默认为字符串类型编码格式为uft-8）
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        // 消费者组ID,这里使用jobName
        kafkaParams.put("group.id", jobName);
        // 指定从earliest(若有无提交offset,则重新开始)/latest(若有无提交offset,则从新产生开始)/none(报异常)
        kafkaParams.put("auto.offset.reset", "latest");
        // 如果true,consumer定期地往zookeeper写入每个分区的offset
        kafkaParams.put("enable.auto.commit", false);

        return kafkaParams;
    }

    public static Map<String, Object> getProducerParams() {
        Map<String, Object> kafkaParams = new HashMap<>();
        // broker的地址清单，建议至少填写两个，避免宕机
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        // acks指定必须有多少个分区副本接收消息，生产者才认为消息写入成功，用户检测数据丢失的可能性
        // acks=0：生产者在成功写入消息之前不会等待任何来自服务器的响应。无法监控数据是否发送成功，但可以以网络能够支持的最大速度发送消息，达到很高的吞吐量。
        // acks=1：只要集群的首领节点收到消息，生产者就会收到来自服务器的成功响应。
        // acks=all：只有所有参与复制的节点全部收到消息时，生产者才会收到来自服务器的成功响应。这种模式是最安全的，
        kafkaParams.put("acks", "all");
        // retries：生产者从服务器收到的错误有可能是临时性的错误的次数
        kafkaParams.put("retries", 0);
        // batch.size：该参数指定了一个批次可以使用的内存大小，按照字节数计算（而不是消息个数)。
        kafkaParams.put("batch.size", 16384);
        // linger.ms：该参数指定了生产者在发送批次之前等待更多消息加入批次的时间，增加延迟，提高吞吐量
        kafkaParams.put("linger.ms", 1);
        // buffer.memory该参数用来设置生产者内存缓冲区的大小，生产者用它缓冲要发送到服务器的消息。
        kafkaParams.put("buffer.memory", 33554432);
        // compression.type:数据压缩格式，有snappy、gzip和lz4，snappy算法比较均衡，gzip会消耗更高的cpu，但压缩比更高
        // key和value的序列化
        kafkaParams.put("key.serializer", StringSerializer.class);
        kafkaParams.put("value.serializer", StringSerializer.class);
        // client.id：该参数可以是任意的字符串，服务器会用它来识别消息的来源。
        // max.in.flight.requests.per.connection：生产者在收到服务器晌应之前可以发送多少个消息。越大越占用内存，但会提高吞吐量
        // timeout.ms：指定了broker等待同步副本返回消息确认的时间
        // request.timeout.ms：生产者在发送数据后等待服务器返回响应的时间
        // metadata.fetch.timeout.ms：生产者在获取元数据（比如目标分区的首领是谁）时等待服务器返回响应的时间。
        // max.block.ms：该参数指定了在调用 send（）方法或使用 partitionsFor（）方法获取元数据时生产者阻塞时间
        // max.request.size：该参数用于控制生产者发送的请求大小。
        // receive.buffer.bytes和send.buffer.bytes：指定了 TCP socket 接收和发送数据包的缓冲区大小，默认值为-1
        return kafkaParams;
    }

}
