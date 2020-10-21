package demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

public class CustomerDemo {
    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("group.id", "group-2");
        //session.timeout.ms：消费者在被认为死亡之前可以与服务器断开连接的时间，默认是3s 。
        properties.put("session.timeout.ms", "30000");
        //消费者是否自动提交偏移量，默认值是true,避免出现重复数据和数据丢失，可以把它设为 false。
        properties.put("enable.auto.commit", "false");
        properties.put("auto.commit.interval.ms", "1000");
        //auto.offset.reset:消费者在读取一个没有偏移量的分区或者偏移量无效的情况下的处理
        //earliest：在偏移量无效的情况下，消费者将从起始位置读取分区的记录。
        //latest：在偏移量无效的情况下，消费者将从最新位置读取分区的记录
        properties.put("auto.offset.reset", "earliest");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // max.partition.fetch.bytes：服务器从每个分区里返回给消费者的最大字节数
        //fetch.max.wait.ms:消费者等待时间，默认是500。
        // fetch.min.bytes:消费者从服务器获取记录的最小字节数。
        // client.id：该参数可以是任意的字符串，服务器会用它来识别消息的来源。
        // max.poll.records:用于控制单次调用 call （） 方住能够返回的记录数量
        //receive.buffer.bytes和send.buffer.bytes：指定了 TCP socket 接收和发送数据包的缓冲区大小，默认值为-1

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        kafkaConsumer.subscribe(Arrays.asList("test"));
        while (true) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, value = %s", record.offset(), record.value());
                System.out.println("=====================>");
            }
        }

    }
}
