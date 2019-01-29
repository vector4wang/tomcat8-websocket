package com.websocket.config;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import javax.servlet.http.HttpSession;

public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator  {
	
	@Override
	public void modifyHandshake(ServerEndpointConfig config,HandshakeRequest request, HandshakeResponse response) {
		HttpSession httpSession = (HttpSession) request.getHttpSession();
		config.getUserProperties().put(HttpSession.class.getName(), httpSession);
	}
	
}
