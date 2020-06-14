package cn.genlei.demo.controller;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author junfeng
 */
@Component
@ServerEndpoint(value = "/ws")
public class WebSocketController {
    static Map<String,Session> activeMap = new ConcurrentHashMap<>();

    static Logger log = Logger.getLogger("WebSocketController");
    /**
     * when websocket connected.
     * @param session
     */
    @OnOpen
    public void onOpen(Session session) {
        Map<String, List<String>> map = session.getRequestParameterMap();
        List<String> list = map.get("username");
        if(list==null || list.size()==0){
            sendMessage(session,"no username! authentication failed.");
            close(session);
            return;
        }
        String username = list.get(0);
        String message = "[" + username + "] join in ！";
        session.getUserProperties().put("username",username);
        addSession(session);
        sendMessageForAll(message);
    }

    /**
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        log.info("onClose, " + session.getUserProperties().get("username"));
        String message = "Close...";
        close(session);
    }

    /**
     * @param session
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("onMessage, " + message);
        String username =  (String)session.getUserProperties().get("username");
        String msg = username + " said: " + message;
        sendMessageForAll(msg);
    }

    /**
     * @param session
     * @param throwable
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        close(session);
        throwable.printStackTrace();
    }

    public void addSession(Session session) {
        activeMap.put(session.getId(),session);
    }

    public void sendMessageForAll(String message) {
        activeMap.forEach((sessionId, session) -> sendMessage(session, message));
    }

    private void sendMessage(Session session, String message) {
        if(session==null){
            return;
        }
        try {
            RemoteEndpoint.Async async = session.getAsyncRemote();
            async.sendText(message);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void close(Session session) {
        try {
            activeMap.remove(session.getId());
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}