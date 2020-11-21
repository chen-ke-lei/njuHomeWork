package webserver.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import webserver.handle.HeroMatchesHandle;
import webserver.handle.PlayerWinHandle;
import webserver.handle.SiteMessageHandle;
import webserver.kafka.ConditionProducer;
import webserver.kafka.ControllableConsumer;
import webserver.socket.MessageSocket;
import webserver.util.CommenUtil;
import webserver.util.ConditionMsg;
import webserver.util.ResultMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    HeroMatchesHandle heroMatchesHandle;

    private static final Map<String, ControllableConsumer> consumers = new ConcurrentHashMap<>();
    private static final Map<String, ConditionProducer> producers = new ConcurrentHashMap<>();

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

    @PostMapping("/start_consumer")
    @ResponseBody
    public ResultMsg start(@RequestBody ConditionMsg msg) {
        if (msg == null) return new ResultMsg(ResultMsg.FAILURE, "param is Null");
        if (CommenUtil.isEmptyString(msg.getTopic())) return new ResultMsg(ResultMsg.FAILURE, "topic is Null");
        if (CommenUtil.isEmptyString(msg.getGroupId())) return new ResultMsg(ResultMsg.FAILURE, "groupId is Null");
        if (!MessageSocket.socketExists(msg.getTopic() + "_" + msg.getGroupId()))
            return new ResultMsg(ResultMsg.FAILURE, "socket has not established");


        //生成者逻辑开启
        synchronized (this) {
            if (producers.containsKey(msg.getTopic())) {
                producers.get(msg.getTopic()).close();
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ConditionProducer producer = new ConditionProducer(msg);
        new Thread(producer).start();
        producers.put(msg.getTopic(), producer);

        String socketname = msg.getTopic() + "_" + msg.getGroupId();
        if (null != consumers.getOrDefault(socketname, null))
            return new ResultMsg(ResultMsg.SUCCESS, "consumer exists");

        ControllableConsumer cc = new ControllableConsumer(socketname, msg.getTopic(),
                records -> {
                    if (socketname.startsWith("siteMessage")) {
                        MessageSocket.sendMessage(socketname, siteMessageHandle.handle(records).values().toString());
                        return null;
                    } else if (socketname.startsWith("playerWin")) {
                        System.out.println("22112");
                        MessageSocket.sendMessage(socketname, playerWinHandle.handle(records));
                        return null;
                    } else if (socketname.startsWith("heroMatches")) {
                        MessageSocket.sendMessage(socketname, heroMatchesHandle.handle(records));
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
        String topic = socketname.split("_")[0];
        ConditionProducer producer = producers.get(topic);
        if (producer != null) {
            producer.close();
            return new ResultMsg(ResultMsg.SUCCESS, "producer stops");
        }
//        ControllableConsumer cc = consumers.getOrDefault(socketname, null);
//        if (null != cc) {
//            cc.stop();
//            return new ResultMsg(ResultMsg.SUCCESS, "consumer stops");
//        } else
            return new ResultMsg(ResultMsg.FAILURE, "producer not exists");
    }


}
