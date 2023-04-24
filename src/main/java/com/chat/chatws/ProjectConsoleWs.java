package com.chat.chatws;

import com.chat.cache.UserCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/websocket/{sessionId}")
public class ProjectConsoleWs {
    public static ProjectConsoleWs service = new ProjectConsoleWs();
    /**
     * 存放所有在线的客户端 [sessionId, session]
     */

    @Resource
    private  UserCache userCache;
    private static final Map<String, Session> clients = new ConcurrentHashMap<>();

    public void sendMessage(String sessionId, String message) {
        if (!clients.containsKey(sessionId)) {
            log.error("未找到指定客户端{}的websocket连接!", sessionId);
            return;
        }
        this.onMessage(sessionId, message, clients.get(sessionId));
        log.info("准备向客户端程序{}发送消息:{}", sessionId, message);
    }



    @OnMessage
    public void onMessage(@PathParam("sessionId") String sessionId, String message, Session session) {
        try {
            userCache.userOnline(sessionId);
            if ("ping".equals(message)) {
                session.getBasicRemote().sendText("pong");
            }

//            if (!StrUtil.isEmpty(message)&&message.contains("id:")) {
//                String userCode = message.substring(3);
//                User user = UserCache.getUser(userCode);
//
//                session.getBasicRemote().sendText("pang");
//            }
            //session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("消息发送失败!", e);
        }
    }

    @OnOpen
    public void onOpen(@PathParam("sessionId") String sessionId, Session session) {
        if (clients.containsKey(sessionId)) {
            log.warn("客户端程序{}已有连接,无需建立连接", sessionId);
            return;
        }
        try {
            if (userCache.userOnlineGet(sessionId)==0) {
                log.warn("无法识别的用户:{}", sessionId);
                session.close();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        session.setMaxIdleTimeout(53 * 1000);
       userCache.userOnline(sessionId);
        clients.put(sessionId, session);
        log.info("客户端程序{}建立连接成功!------>当前在线人数为：{}", sessionId, getOnlineCount());
    }

    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId, Session session) {
        if (clients.containsKey(sessionId)) {
            userCache.userOnlineDel(sessionId);
            clients.remove(sessionId);
            log.info("客户端程序{}断开连接成功!------>当前在线人数为：{}", sessionId, getOnlineCount());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.info("连接{}发生错误!", session.getId());
        throwable.printStackTrace();
    }

    public boolean sessionIsOpen(String sessionId) {
        return clients.get(sessionId).isOpen();
    }

    public synchronized int getOnlineCount() {
        return clients.size();
    }

}