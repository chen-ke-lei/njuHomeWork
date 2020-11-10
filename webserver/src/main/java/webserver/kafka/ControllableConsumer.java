package webserver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import webserver.util.KafKaUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ControllableConsumer implements Runnable {

    // 每个线程维护私有的kafkaConsumer实例
    private final KafkaConsumer<String, String> consumer;

    private String groupId;

    private String topic;

    private long startOffset;

    private Function<ConsumerRecords<String, String>, Void> processRecord;

    public ControllableConsumer(String groupId, String topic, Function<ConsumerRecords<String, String>, Void> func) {
        this.groupId = groupId;
        this.topic = topic;
        this.consumer = new KafkaConsumer<>(KafKaUtil.getConsumerParams(groupId));
        // consumer.subscribe(Collections.singletonList(topic));
        // 这里不使用subscribe方法，而是自己指定分区，是为了能够获取offset/seek等操作
        // 在本项目中默认只使用一个分区来保证时序性
        TopicPartition topicPartition = new TopicPartition(topic, 0);
        List<TopicPartition> topics = Collections.singletonList(topicPartition);
        this.consumer.assign(topics);
        this.startOffset = consumer.position(new TopicPartition(topic, 0));
        this.processRecord = func;
    }

    public ControllableConsumer cloneSelf() {
        ControllableConsumer ccc =  new ControllableConsumer(this.groupId, this.topic, this.processRecord); // ClonedControllableConsumer
        ccc.consumer.seek(new TopicPartition(ccc.topic, 0), this.startOffset);
        return ccc;
    }

    @Override
    public void run() {
        while (!stop) {
            while (suspend) {
                this.onSuspend();
            }
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(50));
            this.processRecord.apply(records);
        }
        consumer.close();
    }

    private final Object lock = new Object();

    private boolean suspend = false;

    private boolean stop = false;

    public void resume() {
        suspend = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void onSuspend() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void suspend() {
        this.suspend = true;
    }

    public void stop() {
        this.stop = true;
    }

}