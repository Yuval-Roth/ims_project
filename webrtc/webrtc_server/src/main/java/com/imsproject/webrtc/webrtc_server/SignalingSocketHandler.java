package com.imsproject.webrtc.webrtc_server;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SignalingSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Here you’ll process incoming messages and forward to appropriate peers.
        // Message format can be JSON: { "type": "offer/answer/candidate", "to": "peerId", "data": {...} }
        String payload = message.getPayload();

        // Example: Forwarding message to the target peer
        String targetSessionId = ... // Parse this from your payload
        WebSocketSession targetSession = sessions.get(targetSessionId);

        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(payload));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }
}

