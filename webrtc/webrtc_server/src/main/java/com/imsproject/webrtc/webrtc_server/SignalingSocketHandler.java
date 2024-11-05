package com.imsproject.webrtc.webrtc_server;

import com.imsproject.utils.WebRTCRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SignalingSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        // Parse the message
        String rawPayload = message.getPayload();
        WebRTCRequest request = WebRTCRequest.fromJson(rawPayload);

        // Get the target peer session
        String targetSessionId = request.to();
        WebSocketSession targetSession = sessions.get(targetSessionId);

        // Send the message to the target peer
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(request.data()));
        }
    }

    @SuppressWarnings("resource")
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }
}

