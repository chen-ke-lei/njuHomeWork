package webserver.kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import webserver.util.CommenUtil;
import webserver.util.ConditionMsg;
import webserver.util.FileUtil;
import webserver.util.KafKaUtil;

public class ConditionProducer implements Runnable {
    private ConditionMsg msg;
    private Producer<String, String> producer;
    String send_topic;
    private boolean stop;

   private boolean filterByFileIndex(String fileIndex) {
        if (CommenUtil.isEmptyString(fileIndex)) return false;
        String start = msg.getStart();
        String end = msg.getEnd();
        if (!CommenUtil.isEmptyString(start) && fileIndex.compareTo(start.replaceAll("-","")) < 0) return false;
        if (!CommenUtil.isEmptyString(end) && fileIndex.compareTo(end.replaceAll("-","")) > 0) return false;
        return true;
    }

   private boolean filterByFileContent(String content) {
        if (CommenUtil.isEmptyString(content)) return false;
        if (CommenUtil.isEmptyString(msg.getHero())) return true;
        try {
            JSONObject jsonObject = JSONObject.parseObject(content);
            JSONArray player = jsonObject.getJSONObject("blue_team").getJSONArray("players");
            for (Object o : player) {
                if (msg.getHero().equals(((JSONObject) o).getString("hero_id"))
                        || msg.getHero().equals(((JSONObject) o).getString("hero_name")))
                    return true;
            }

            player = jsonObject.getJSONObject("red_team").getJSONArray("players");
            for (Object o : player) {
                if (msg.getHero().equals(((JSONObject) o).getString("hero_id"))
                        || msg.getHero().equals(((JSONObject) o).getString("hero_name")))
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public ConditionProducer(ConditionMsg conditionMsg) {
        this.msg = conditionMsg;
        send_topic = "source-" + conditionMsg.getTopic();
        producer = new KafkaProducer<>(KafKaUtil.getProducerParams());
        stop = false;
    }

    public void close() {
        stop = true;
    }

    @Override
    public void run() {
        System.out.println("生成者线程以开启" + msg);
        String init = FileUtil.readFile(FileUtil.INIT_PATH);
        producer.send(new ProducerRecord<>(this.send_topic, init));
      //  System.out.println(init);

        try {

            String date2matchJsonStr = FileUtil.readFile(FileUtil.INDEX_PATH);
            JSONObject date2matchJson = JSONObject.parseObject(date2matchJsonStr, Feature.OrderedField);
            int i = 0;
            for (String dateStr : date2matchJson.keySet()) {
                if (dateStr.equals("unknown")) continue;
                if (!filterByFileIndex(dateStr)) continue;
                JSONObject racesJson = date2matchJson.getJSONObject(dateStr);
                for (String raceId : racesJson.keySet()) {
                    JSONArray matchesJson = racesJson.getJSONArray(raceId);
                    for (String matchId : matchesJson.toJavaList(String.class)) {
                        if (stop) {
                            return;
                        }
                        String filePath = FileUtil.getMatchPath(dateStr, matchId);
                        String content = FileUtil.readFile(filePath);
                        if (!filterByFileContent(content)) continue;
                        producer.send(new ProducerRecord<>(this.send_topic, content));
                        i++;
                        if (i % 200 == 0)
                            Thread.sleep(300);
                    }
                    // Thread.sleep(500);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            System.out.println("生成者线程以停止" + msg);

            if (producer != null) producer.close();
        }

    }
}
