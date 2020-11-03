package kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import util.FileUtil;
import util.KafKaUtil;

import java.util.Map;

public class MatchProducer implements Runnable {

    @Override
    public void run() {
        Producer<String, String> producer = null;
        try {
            Map<String, Object> kafkaParams = KafKaUtil.getProducerParams();
            producer = new KafkaProducer<>(kafkaParams);

            String date2matchJsonStr = FileUtil.readFile(FileUtil.INDEX_PATH);
            JSONObject date2matchJson = JSONObject.parseObject(date2matchJsonStr, Feature.OrderedField);

            for (String dateStr : date2matchJson.keySet()) {
                if (dateStr.equals("unknown")) continue;
                JSONObject racesJson = date2matchJson.getJSONObject(dateStr);
                for (String raceId : racesJson.keySet()) {
                    JSONArray matchesJson = racesJson.getJSONArray(raceId);
                    for (String matchId : matchesJson.toJavaList(String.class)) {
                        String filePath = FileUtil.getMatchPath(dateStr, matchId);
                        producer.send(new ProducerRecord<>(KafKaUtil.SOURCE_TOPIC, FileUtil.readFile(filePath)));
                    }
                    // Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (producer != null)
                producer.close();
        }
    }
}
