package webserver.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HeroMatchesHandle {

    public String handle(ConsumerRecords<String, String> records) {
        Map<String, JSONObject> results = new LinkedHashMap<>();
        try {
            for (ConsumerRecord<String, String> record : records) {
                JSONObject heroMatches = JSON.parseObject(record.value());
                if (heroMatches != null) {
                    String date = heroMatches.getString("updateTime");
                    if (date.equals("99999999")) continue;
                    String hero = heroMatches.getString("hero");
                    if (hero == null || hero.length() == 0) continue;
                    if (!results.containsKey(hero))
                        results.put(hero, heroMatches);
                    else {
                        String lastDate = results.get(hero).getString("updateTime");
                        if (lastDate == null || lastDate.compareTo(date) <= 0)
                            results.put(hero, heroMatches);
                    }
                }
            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return JSONObject.toJSONString(results.values());
    }
}
