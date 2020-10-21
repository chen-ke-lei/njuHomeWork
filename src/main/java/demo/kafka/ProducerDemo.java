package demo.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import util.KafKaUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class ProducerDemo {
    public static void main(String[] args) {

        Producer<String, String> producer = null;
        try {
            Properties properties=new KafKaUtil().initKafKaProperties();
            producer = new KafkaProducer<String, String>(properties);

            File baseDir = new File("C:\\bigdata\\datsets\\matches_simplified\\matches_simplified");

            for (File dir : baseDir.listFiles()) {
                if (!dir.isDirectory()) continue;
                if (dir.getName().contains("unknown")) continue;
                for (File file : dir.listFiles()) {
                    if (!file.getName().endsWith(".json")) continue;
                    StringBuffer buffer = new StringBuffer();
                    String line;
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    producer.send(new ProducerRecord<String, String>(KafKaUtil.topic,buffer.toString()));
                }
                Thread.sleep(15*1000);
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            producer.close();
        }

    }
}
