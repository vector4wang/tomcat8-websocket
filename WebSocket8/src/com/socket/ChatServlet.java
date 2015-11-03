package com.socket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import utils.MessageUtil;



@ServerEndpoint(value = "/websocket",configurator=GetHttpSessionConfigurator.class)
public class ChatServlet {


	private static final Map<HttpSession,ChatServlet> onlineUsers = new HashMap<HttpSession, ChatServlet>();

	private static int onlineCount = 0;

	private HttpSession httpSession;

	private Session session;


	@OnOpen
	public void onOpen(Session session,EndpointConfig config){

		this.session = session;
		this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		if(httpSession.getAttribute("user") != null){
			onlineUsers.put(httpSession, this);
		}
		String names = getNames();
		String content = MessageUtil.sendContent(MessageUtil.USER,names);
		broadcastAll(content);
		addOnlineCount();           //在线数加1
		System.out.println("有新连接加入!当前在线人数为" + onlineUsers.size());
	}

	@OnClose
	public void onClose(){
		if(onlineUsers.containsKey(this.httpSession)){
			onlineUsers.remove(this);  //从set中删除
		}
		subOnlineCount();           //在线数减1   
		System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
	}

	@OnMessage
	public void onMessage(String message, Session session) throws IOException {

		HashMap<String,String> messageMap = MessageUtil.getMessage(message);    //处理消息类
		String fromName = messageMap.get("fromName");    //消息来自人 的userId
		String toName = messageMap.get("toName");       //消息发往人的 userId
		String mapContent = messageMap.get("content");
		String type = messageMap.get("type");
		if(MessageUtil.MESSAGE.equals(type)){
			if(toName.isEmpty()){
				sendOffLine(fromName,toName);
				return;
			}

			if("all".equals(toName)){
				String msgContentString = fromName + "对所有人说: " + mapContent;   //构造发送的消息
				String content = MessageUtil.sendContent(MessageUtil.MESSAGE,msgContentString);
				broadcastAll(content);
			}else{
				try {
					String content = MessageUtil.sendContent(MessageUtil.MESSAGE,mapContent);
					singleChat(fromName,toName,content);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else if(MessageUtil.COORD.equals(type)){
			System.out.println(mapContent);
			drawPanel(fromName, toName, mapContent);
			
			System.out.println("画图模式");
			
		}

		
		

		//System.out.println("来自客户端的消息:" + message);
		//broadcastAll(message);
	}

	private void drawPanel(String fromName, String toName, String mapContent) throws IOException {
		
		String contentTemp = MessageUtil.sendContent(MessageUtil.COORD,mapContent.toString());
		for (HttpSession key : onlineUsers.keySet()) {
			if(key.getAttribute("user").equals(toName)){
				onlineUsers.get(key).session.getBasicRemote().sendText(contentTemp);
			}
		}
		
	}

	private void singleChat(String fromName, String toName, String mapContent) throws IOException {
		String msgContentString = fromName + "对" + toName + "说: " + mapContent;
		String contentTemp = MessageUtil.sendContent(MessageUtil.MESSAGE,msgContentString);
		boolean isExit = false;
		for (HttpSession key : onlineUsers.keySet()) {
			if(key.getAttribute("user").equals(toName)){
				isExit = true;
			}
		}
		if(isExit){
			for (HttpSession key : onlineUsers.keySet()) {
				if(key.getAttribute("user").equals(fromName) || key.getAttribute("user").equals(toName)){
					onlineUsers.get(key).session.getBasicRemote().sendText(contentTemp);
				}
			}
		}else{
			String content = MessageUtil.sendContent(MessageUtil.MESSAGE,"客服不在线请留言...");
			broadcastAll(content);
		}

	}
	private void sendOffLine(String fromName, String toName) throws IOException {
		String msgContentString = toName + "不在线";
		String content = MessageUtil.sendContent(MessageUtil.MESSAGE,msgContentString);
		for (HttpSession key : onlineUsers.keySet()) {
			if(key.getAttribute("user").equals(fromName) || key.getAttribute("user").equals(toName)){
				onlineUsers.get(key).session.getBasicRemote().sendText(content);
			}
		}
	}
	private static void broadcastAll(String msg) {
		for (HttpSession key : onlineUsers.keySet()) {
			try {
				onlineUsers.get(key).session.getBasicRemote().sendText(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@OnError
	public void onError(Session session, Throwable error){
		System.out.println("发生错误");
		error.printStackTrace();
	}


	private String getNames() {
		String names = "";
		for (HttpSession key : onlineUsers.keySet()) {
			String name = (String) key.getAttribute("user");
			names += name + ",";
		}
		String namesTemp = names.substring(0,names.length()-1);
		return namesTemp;
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