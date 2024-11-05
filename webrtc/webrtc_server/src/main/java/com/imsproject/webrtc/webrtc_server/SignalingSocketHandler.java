package com.imsproject.webrtc.webrtc_server;

import com.imsproject.utils.Response;
import com.imsproject.utils.SimpleIdGenerator;
import com.imsproject.utils.webrtc.WebRTCRequest;
import com.imsproject.utils.webrtc.WebRTCRequest.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
@SuppressWarnings("resource")
public class SignalingSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final SimpleIdGenerator idGenerator = new SimpleIdGenerator(2);

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.debug("New connection: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        // Parse the message
        String rawPayload = message.getPayload();

        WebRTCRequest request;
        try{
            request = WebRTCRequest.fromJson(rawPayload);
        } catch (Exception ignored){
            log.error("Received Invalid message: {}\nfrom {}", rawPayload, session.getId());
            return;
        }

        WebSocketSession targetSession = null;
        String messageToTarget = null;

        // Handle the message
        switch(request.type()){
            case ENTER -> {
                targetSession = session;
                String id = idGenerator.generate();
                messageToTarget = WebRTCRequest.builder()
                        .setType(Type.ENTER)
                        .setTo(id)
                        .build()
                        .toJson();
                log.debug("New peer entered: {}", id);
            }
            case EXIT -> session.close();
            default -> {
                targetSession = sessions.get(request.to());
                messageToTarget = rawPayload;
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
        log.debug("Peer exited: {}", session.getId());
    }


    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
        log.debug("Signaling server started");
    }
}

