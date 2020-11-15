package webserver;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import webserver.handle.PlayerWinHandle;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebServerApp {

    public static void main(String[] args) {

        SpringApplication.run(WebServerApp.class);
    }

}
