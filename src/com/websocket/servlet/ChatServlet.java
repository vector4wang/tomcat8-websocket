package com.websocket.servlet;

import com.alibaba.fastjson.JSON;
import com.websocket.config.GetHttpSessionConfigurator;
import com.websocket.model.Message;
import com.websocket.utils.MessageUtil;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;


@ServerEndpoint(value = "/websocket", configurator = GetHttpSessionConfigurator.class)
public class ChatServlet {

    private static Map<HttpSession, ChatServlet> onlineUsers = new HashMap<HttpSession, ChatServlet>();

    private static int onlineCount = 0;

    private HttpSession httpSession;

    private Session session;


    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {

        this.session = session;
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession.getAttribute("user") != null) {
            onlineUsers.put(httpSession, this);
        }
        Message message = new Message();
        message.setCreateTime(new Date().getTime());
        message.setUserList(getNames());
        message.setMsgType(MessageUtil.USER);
        broadcastAll(message);
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入!当前在线人数为" + onlineUsers.size());
    }


    @OnClose
    public void onClose() {
        if (onlineUsers.containsKey(this.httpSession)) {
            onlineUsers.remove(this);  //从set中删除
        }
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    @OnMessage
    public void onMessage(String msg) throws IOException {
        System.out.println(msg);
        Message message = JSON.parseObject(msg, Message.class);
        if (MessageUtil.TEXT.equals(message.getMsgType())) {
            if (StringUtils.isBlank(message.getToUserName())) {
                sendOffLine(message.getFromUserName(), message.getToUserName());
                return;
            }

            if ("all".equals(message.getToUserName())) {
                String msgContentString = message.getFromUserName() + "对所有人说: " + message.getContent();   //构造发送的消息
                broadcastAll(msgContentString,null);
            } else {
                try {
                    singleChat(message.getFromUserName(), message.getToUserName(), message.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (MessageUtil.COORD.equals(message.getMsgType())) {
            drawPanel(message.getFromUserName(), message.getToUserName(), message.getContent());
        }


        //System.out.println("来自客户端的消息:" + message);
        //broadcastAll(message);
    }

    private void drawPanel(String fromName, String toName, String content) throws IOException {
        Message message = new Message();
        message.setFromUserName(fromName);
        message.setCreateTime(new Date().getTime());
        message.setContent(content);
        message.setToUserName(toName);
        message.setMsgType(MessageUtil.COORD);
        if ("all".equals(toName)) {
            singleChat(message);
        } else {
            broadcastAll(message);
        }

    }


    private void singleChat(Message message) throws IOException {
        boolean isExit = false;
        for (HttpSession key : onlineUsers.keySet()) {
            if (key.getAttribute("user").equals(message.getToUserName())) {
                isExit = true;
            }
        }
        if (isExit) {
            for (HttpSession key : onlineUsers.keySet()) {
                if (key.getAttribute("user").equals(message.getFromUserName()) || key.getAttribute("user").equals(message.getToUserName())) {
                    onlineUsers.get(key).session.getBasicRemote().sendText(JSON.toJSONString(message));
                }
            }
        } else {
            broadcastAll("客服不在线请留言...",null);
        }
    }

    /**
     * p2p 单点聊天
     *
     * @param fromName
     * @param toName
     * @param content
     * @throws IOException
     */
    private void singleChat(String fromName, String toName, String content) throws IOException {
        Message message = new Message();
        message.setFromUserName(fromName);
        message.setToUserName(toName);
        message.setContent(content);
        message.setCreateTime(new Date().getTime());
        message.setMsgType(MessageUtil.TEXT);
        boolean isExit = false;
        for (HttpSession key : onlineUsers.keySet()) {
            if (key.getAttribute("user").equals(toName)) {
                isExit = true;
            }
        }
        if (isExit) {
            for (HttpSession key : onlineUsers.keySet()) {
                if (key.getAttribute("user").equals(fromName) || key.getAttribute("user").equals(toName)) {
                    onlineUsers.get(key).session.getBasicRemote().sendText(JSON.toJSONString(message));
                }
            }
        } else {
            broadcastAll("客服不在线请留言...",null);
        }

    }


    /**
     * 掉线信息
     *
     * @param fromName
     * @param toName
     * @throws IOException
     */
    private void sendOffLine(String fromName, String toName) throws IOException {
        Message message = new Message();
        message.setFromUserName(fromName);
        message.setToUserName(toName);
        message.setCreateTime(new Date().getTime());
        message.setMsgType(MessageUtil.TEXT);
        message.setContent(fromName + "已下线");
//        for (HttpSession key : onlineUsers.keySet()) {
//            if (key.getAttribute("user").equals(fromName) || key.getAttribute("user").equals(toName)) {
//                onlineUsers.get(key).session.getBasicRemote().sendText(content);
//            }
//        }
        broadcastAll(fromName + "已下线",fromName);
    }

    /**
     * 广播
     *
     * @param msg
     */
    private static void broadcastAll(String msg, String fromUserName) {
        Message message = new Message();
        message.setFromUserName(StringUtils.isNotBlank(fromUserName) ? fromUserName : "admin");
        for (HttpSession key : onlineUsers.keySet()) {
            try {
                message.setCreateTime(new Date().getTime());
                message.setContent(msg);
                message.setMsgType(MessageUtil.TEXT);
                onlineUsers.get(key).session.getBasicRemote().sendText(JSON.toJSONString(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastAll(Message message) {
        for (HttpSession key : onlineUsers.keySet()) {
            try {
                onlineUsers.get(key).session.getBasicRemote().sendText(JSON.toJSONString(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }


    /**
     * 获取当前用户在线集合
     *
     * @return
     */
    private List<String> getNames() {
        List<String> result = new ArrayList<>();
        for (HttpSession key : onlineUsers.keySet()) {
            String name = (String) key.getAttribute("user");
            result.add(name);
        }
        return result;
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        ChatServlet.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        ChatServlet.onlineCount--;
    }

}