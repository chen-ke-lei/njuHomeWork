package webserver.socket;

import org.springframework.stereotype.Component;
import webserver.controller.ConsumerController;
import webserver.util.KafKaUtil;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一种可行的顺序是？？？
 * 前端图表挂载时初始化通道名称，并在生命周期内不变（刷新后会改变？）
 * 之后通过ConsumerController开始一个Consumer进程(其group_id为socketname，包含topic信息)
 * 之后通过socket接受信息并绘图
 * ConsumerController还提供暂停，重绘(将offset重置为0) (这两者暂无需求)
 * 因此会产生很多不同Consumer Group(与客户端socket对应)，因时间戳而唯一，以此来实现多端都可以观看图表
 * 以上想法仅为完成项目，实际生产绝不可行
 * ws://domain:port/websocket/{socketname}
 */
@Component
@ServerEndpoint("/websocket/{socketname}")
public class MessageSocket {

    /**
     * 以通道名称为key，连接会话为对象保存起来
     * 通道名称key为Kafka topic + "_" + timestamp
     * 在前端页面加载时确定通道名称，并在页面生命周期内保持不变
     */
    private static Map<String, Session> websocketClients = new ConcurrentHashMap<>();
    /**
     * 会话
     */
    private Session session;
    /**
     * 通道名称
     */
    private String socketname;

    public static boolean socketExists(String socketname) {
        return websocketClients.containsKey(socketname);
    }

    /**
     * 发送消息到指定连接
     *
     * @param socketname 连接名
     * @param jsonString 消息
     */
    public static void sendMessage(String socketname, String jsonString) {
        Session nowsession = websocketClients.get(socketname);
        if (nowsession != null) {
            try {
                nowsession.getBasicRemote().sendText(jsonString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnOpen
    public void onOpen(@PathParam("socketname") String socketname, Session session) {
        try {
            this.socketname = socketname;
            this.session = session;
            if (websocketClients.get(socketname) == null) {
                websocketClients.put(socketname, session);
                System.out.println("socket通道" + socketname + "已加入连接" + "---" +
                        "session id" + session.getId());
            }

            String[] parts = socketname.split("_");
            if (parts.length != 2 || !KafKaUtil.isValidTopic(parts[0])) {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("服务端发生了错误" + error.getMessage());
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose() {
        websocketClients.remove(socketname);
        ConsumerController.cancel(socketname);
        System.out.println("socket通道" + socketname + "已退出连接");
    }

    /**
     * 收到客户端的消息
     *
     * @param message 消息
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到消息：" + message);
    }

}

