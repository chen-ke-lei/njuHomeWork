package webserver.handle;

/**
 * @Author:Wang Mo
 * @Description：
 */
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HeroWinRateHandle {
    public Map<String, JSONObject> handle(ConsumerRecords<String, String> records) {
        Map<String, JSONObject> res = new HashMap<>();
        String latestDate="20100101";
        for (ConsumerRecord<String, String> record : records) {
            try {
                //     System.out.println(record.value());
                JSONObject tmp = JSON.parseObject(record.value());
                if (tmp != null) {
                    String date = tmp.getString("updateTime");
                    if (date.equals("99999999")) continue;
                    if(tmp.containsKey("hero")) {
                        String hero = tmp.getString("hero");
                        if (hero == null || hero.length() == 0) continue;
                        if (!res.containsKey(hero))
                            res.put(hero, tmp);
                        else {
                            String lastDate = res.get(hero).getString("updateTime");
                            if(latestDate.compareTo(date)<0)
                                latestDate=date;
                            if (lastDate == null || lastDate.compareTo(date) <= 0)
                                res.put(hero, tmp);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
//        for(String hero:res.keySet()){
//            if(res.get(hero).getString("updateTime").compareTo(latestDate)<0){
//                JSONObject tmp=res.get(hero);
//                tmp.put("updateTime",latestDate);
//                res.put(hero,tmp);
//            }
//            else {
//                latestDate=res.get(hero).getString("updateTime");
//            }
//        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }
}
