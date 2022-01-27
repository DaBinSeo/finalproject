package com.nb.spring.common.websocket;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RealTimeActionServer extends TextWebSocketHandler {

	private List<WebSocketSession> clients = new ArrayList<WebSocketSession>();
	
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(clients.size());
		System.out.println(message.getPayload());
		String msg = message.getPayload();
		for(WebSocketSession ss : clients) {
			ss.sendMessage(message);
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// TODO Auto-generated method stub
		if(clients.size()<=0) {
			clients.add(session);
		}else {
			for(WebSocketSession ss : clients) {
				if(ss.getId()!=session.getId()) {
					clients.add(session);
				}
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		clients.remove(session);
	}
	
}