package demo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;

import java.io.*;

public class TestFile {
    public static void main(String[] args) {
        String filePath = "C:\\bigdata\\datsets\\matches_simplified\\matches_simplified";
        File dir = new File(filePath);
        int i=0;
        try {
            for (File subDir : dir.listFiles()) {
                if (subDir.isDirectory())
                    for (File file : subDir.listFiles()) {
                        String fileMsg = "";
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            fileMsg += line;
                        }

                        reader.close();
                       i++;
                        try {
                            JSONObject data = JSONObject.parseObject(fileMsg);
                        } catch (Exception e) {
                            System.out.println(file.getPath() + file.getName());
                        }

                    }
                System.out.println("已完成"+subDir.getName()+"以解析文件数"+i);
            }
        } catch (Exception e) {

        }

    }
}
