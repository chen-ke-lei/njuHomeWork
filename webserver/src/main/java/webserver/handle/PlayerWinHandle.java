package webserver.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nan
 * @date 2020/11/14
 */
@Component
public class PlayerWinHandle {

    public Map<String, JSONObject> handle(ConsumerRecords<String, String> records) {
        Map<String, JSONObject> res = new HashMap<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                System.out.println(record.value());
                JSONObject tmp = JSON.parseObject(record.value());
                if (tmp != null) {
                    String playerId = tmp.getString("playerId");
                    if (!res.containsKey(playerId)) res.put(playerId, tmp);
                    else {
                        String lastDate = res.get(playerId).getString("updateTime");
                        if (lastDate == null || lastDate.compareTo(tmp.getString("updateTime")) < 0)
                            res.put(playerId, tmp);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return res;
    }
}
