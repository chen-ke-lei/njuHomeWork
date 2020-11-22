package webserver.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.palette.LinearGradientColorPalette;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author nan
 * @date 2020/11/14
 */
@Component
public class PlayerWinHandle {

    public String handle(ConsumerRecords<String, String> records)  {


        Map<String, JSONObject> res = new HashMap<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                JSONObject tmp = JSON.parseObject(record.value());
                if (tmp != null) {
                    String playerId = tmp.getString("play_id");
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
        System.out.println(res.toString());
        return wordCloud(res);
    }

    public String wordCloud (Map<String, JSONObject> res) {
        //此处不设置会出现中文乱码
        java.awt.Font font = new java.awt.Font("STSong-Light", 2, 18);

        final Dimension dimension = new Dimension(900, 900);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(255));
        wordCloud.setFontScalar(new SqrtFontScalar(12, 42));
        //设置词云显示的三种颜色，越靠前设置表示词频越高的词语的颜色
        wordCloud.setColorPalette(new LinearGradientColorPalette(Color.RED, Color.BLUE, Color.GREEN, 30, 30));

        wordCloud.setKumoFont(new KumoFont(font));
        wordCloud.setBackgroundColor(new Color(255, 255, 255));
        //因为我这边是生成一个圆形,这边设置圆的半径
        wordCloud.setBackground(new CircleBackground(255));

        List<WordFrequency> wordFrequencies = new ArrayList<>();
        for (Map.Entry<String,JSONObject> temp:res.entrySet()){
            String playerName=temp.getValue().getString("play_name");
            int win=temp.getValue().getInteger("winGames");
            if(playerName==null)
            {
                ;
            }else{
                WordFrequency wordFrequency=new WordFrequency(playerName,win);
                System.out.println(wordFrequency.toString());
                wordFrequencies.add(wordFrequency);
            }

        }
        System.out.println(wordFrequencies);
        wordCloud.build(wordFrequencies);
      //  String date=new Date().toString();
        //     wordCloud.writeToFile("/Users/Trayvon/Desktop/test/test" +date + ".png");

        //转换为base64返回
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();//io流
        wordCloud.writeToStreamAsPNG(outputStream);//写入流中
        byte[] bytes = outputStream.toByteArray();//转换成字节
        BASE64Encoder encoder = new BASE64Encoder();
        String png_base64 =  encoder.encodeBuffer(bytes).trim();//转换成base64串
        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
        return png_base64;
    }
    public static String imageToBase64Str() {
        InputStream inputStream = null;
        byte[] data = null;
        try {
            inputStream = new FileInputStream("src/main/java/webserver/handle/test.png");
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 加密
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
}
