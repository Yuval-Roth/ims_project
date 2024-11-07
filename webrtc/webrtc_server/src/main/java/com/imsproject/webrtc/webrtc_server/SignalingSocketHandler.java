package com.imsproject.webrtc.webrtc_server;

import com.imsproject.utils.Response;
import com.imsproject.utils.SimpleIdGenerator;
import com.imsproject.utils.webrtc.WebRTCRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@SuppressWarnings("resource")
public class SignalingSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final SimpleIdGenerator idGenerator = new SimpleIdGenerator(2);

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        // Parse the message
        String rawPayload = message.getPayload();
        WebRTCRequest request = WebRTCRequest.fromJson(rawPayload);

        WebSocketSession targetSession = null;
        String messageToTarget = null;

        // Handle the message
        switch(request.type()){
            case ENTER -> {
                targetSession = session;
                messageToTarget = Response.getOk(idGenerator.generate());
            }
            case OFFER, ANSWER, ICE_CANDIDATES -> {
                targetSession = sessions.get(request.to());
                messageToTarget = rawPayload;
            }
            case EXIT -> {
                sessions.remove(session.getId());
                session.close();
                return;
            }
        }

        // Send the message to the target peer
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(messageToTarget));
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }
}

