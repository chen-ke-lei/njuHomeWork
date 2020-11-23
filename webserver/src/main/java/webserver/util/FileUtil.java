package webserver.util;

import java.io.*;

public class FileUtil {
    public static String INDEX_PATH;
    public static String INIT_PATH;
    public static String INIT_PATH_2;
    public static String PLAYER_HERO_EDGE_PATH;
    public static String SOURCE_PATH = "/Users/Trayvon/Desktop/matches_simplified";
    // public static String SOURCE_PATH = "C:\\bigdata\\datsets\\matches_simplified";

    static {
        INDEX_PATH = FileUtil.class.getClassLoader().getResource("lol_date_match_map.json").getFile();
        INIT_PATH = FileUtil.class.getClassLoader().getResource("end.json").getFile();
        INIT_PATH_2=FileUtil.class.getClassLoader().getResource("endAll.json").getFile();
        PLAYER_HERO_EDGE_PATH = FileUtil.class.getClassLoader().getResource("player_hero_edge.json").getFile();
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

    public static void writeFile(String content, String path) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(content);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
