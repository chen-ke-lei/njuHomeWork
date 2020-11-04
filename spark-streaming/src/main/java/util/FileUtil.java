package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

    //public static String SOURCE_PATH = "/Users/Trayvon/Desktop/matches_simplified";
    public static String SOURCE_PATH = "C:\\bigdata\\datsets\\matches_simplified";

    public static String INDEX_PATH;

    public static String CHECK_POINT_PATH = "C:\\bigdata\\checkpoint";
//    public static String CHECK_POINT_PATH = "/Users/Trayvon/Desktop/streaming_checkpoint";

    static {
        INDEX_PATH = FileUtil.class.getClassLoader().getResource("lol_date_match_map.json").getFile();
    }

    public static String getMatchPath(String dateStr, String matchId) {
        return SOURCE_PATH + File.separator + "matches_" + dateStr.substring(0, 4) + File.separator + matchId + ".json";
    }

    public static String readFile(String path) {
        File file = new File(path);
        StringBuffer buffer = new StringBuffer();
        try (BufferedReader freader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = freader.readLine()) != null) buffer.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}
