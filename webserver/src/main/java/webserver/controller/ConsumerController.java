package webserver.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import webserver.handle.PlayerWinHandle;
import webserver.handle.SiteMessageHandle;
import webserver.kafka.ControllableConsumer;
import webserver.socket.MessageSocket;
import webserver.util.KafKaUtil;
import webserver.util.ResultMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ------------------
 * |start -> stop     |
 * |  |               | -> restart
 * |  V               |
 * | suspend -> resume|
 * ------------------
 * http://domain:port/action?socketname=xxxx
 */
@RestController
public class ConsumerController {
    @Autowired
    SiteMessageHandle siteMessageHandle;
    @Autowired
    PlayerWinHandle playerWinHandle;

    private static final Map<String, ControllableConsumer> consumers = new ConcurrentHashMap<>();

    public static void cancel(String socketname) {
        try {
            ControllableConsumer cc = consumers.getOrDefault(socketname, null);
            if (null != cc) {
                cc.stop();
                Thread.sleep(500);
                consumers.remove(socketname);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/start_consumer")
    public ResultMsg start(@RequestParam String socketname) {
        if (!MessageSocket.socketExists(socketname))
            return new ResultMsg(ResultMsg.FAILURE, "socket has not established");
        String[] parts = socketname.split("_");
        if (parts.length != 2 || !KafKaUtil.isValidTopic(parts[0]))
            return new ResultMsg(ResultMsg.FAILURE, "socketname is invalid");
        String topic = parts[0];
        if (null != consumers.getOrDefault(socketname, null))
            return new ResultMsg(ResultMsg.SUCCESS, "consumer exists");

        ControllableConsumer cc = new ControllableConsumer(socketname, topic,
                records -> {
                    if (socketname.startsWith("siteMessage")) {
                        MessageSocket.sendMessage(socketname, siteMessageHandle.handle(records).values().toString());
                        return null;
                    }
                    else if (socketname.startsWith("playerWin")) {
                        MessageSocket.sendMessage(socketname, playerWinHandle.handle(records));
                        return null;
                    }
                    List<String> recordStrs = new ArrayList<>();
                    for (ConsumerRecord<String, String> record : records) {
                        recordStrs.add(record.value());
//                        System.out.println("当前线程名称 : " + Thread.currentThread().getName() +
//                                ", 主题名称 :" + record.topic() + ", 分区名称 :" + record.partition() +
//                                ", 位移名称 :" + record.offset() + ", value :" + record.value());
                    }
                    MessageSocket.sendMessage(socketname, JSONObject.toJSONString(recordStrs));
                    return null;
                });

        consumers.put(socketname, cc);
        Thread thread = new Thread(cc);
        thread.start();
        return new ResultMsg(ResultMsg.SUCCESS, "consumer starts");
    }

    @GetMapping("/restart_consumer")
    public ResultMsg restart(@RequestParam String socketname) {
        try {
            ControllableConsumer cc = consumers.getOrDefault(socketname, null);
            if (null != cc) {  // 因无法对运行中的consumer线程进行操作，这里直接clone一个新的代替，其offset与cc初始offset相同
                cc.stop();
                Thread.sleep(500);
                ControllableConsumer ccc = cc.cloneSelf();
                consumers.put(socketname, ccc);
                Thread thread = new Thread(ccc);
                thread.start();
                return new ResultMsg(ResultMsg.SUCCESS, "consumer restarts");
            } else
                return new ResultMsg(ResultMsg.FAILURE, "consumer not exists");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ResultMsg(ResultMsg.FAILURE, "exception");
        }
    }

    @GetMapping("/suspend_consumer")
    public ResultMsg suspend(@RequestParam String socketname) {
        ControllableConsumer cc = consumers.getOrDefault(socketname, null);
        if (null != cc) {
            cc.suspend();
            return new ResultMsg(ResultMsg.SUCCESS, "consumer suspends");
        } else
            return new ResultMsg(ResultMsg.FAILURE, "consumer not exists");
    }

    @GetMapping("/resume_consumer")
    public ResultMsg resume(@RequestParam String socketname) {
        ControllableConsumer cc = consumers.getOrDefault(socketname, null);
        if (null != cc) {
            cc.resume();
            return new ResultMsg(ResultMsg.SUCCESS, "consumer resumes");
        } else
            return new ResultMsg(ResultMsg.FAILURE, "consumer not exists");
    }

    @GetMapping("/stop_consumer")
    public ResultMsg stop(@RequestParam String socketname) {
        ControllableConsumer cc = consumers.getOrDefault(socketname, null);
        if (null != cc) {
            cc.stop();
            return new ResultMsg(ResultMsg.SUCCESS, "consumer stops");
        } else
            return new ResultMsg(ResultMsg.FAILURE, "consumer not exists");
    }
}
