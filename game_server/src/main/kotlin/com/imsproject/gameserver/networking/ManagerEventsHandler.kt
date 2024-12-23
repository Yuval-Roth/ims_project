package com.imsproject.gameserver.networking

import com.imsproject.gameserver.HEARTBEAT_TIMEOUT_THRESHOLD
import com.imsproject.gameserver.ManagerEvent
import com.imsproject.gameserver.isMoreThanSecondsAgo
import com.imsproject.gameserver.send
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class ManagerEventsHandler : TextWebSocketHandler() {

    val observers = ConcurrentHashMap<String,WebSocketSession>()
    val heartBeats = ConcurrentHashMap<String, LocalDateTime>()

    fun notify(event: ManagerEvent){
        val message = event.toJson()
        val iter = observers.iterator()
        while (iter.hasNext()){
            val entry = iter.next()
            val session = entry.value

            // check if the session is dead and remove it if so
            val lastHeartBeat = heartBeats[session.id]!!
            if(lastHeartBeat.isMoreThanSecondsAgo(HEARTBEAT_TIMEOUT_THRESHOLD)){
                session.close(CloseStatus.GOING_AWAY)
                heartBeats.remove(session.id)
                iter.remove()
                continue
            }

            // send the message
            session.send(message)
        }
    }

    override fun afterConnectionEstablished(@NonNull session: WebSocketSession) {
        println("New manager events listener connected with id: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val rawPayload: String = message.payload

        // Parse the message
        val eventMessage: ManagerEvent
        try {
            eventMessage = ManagerEvent.fromJson(rawPayload)
        } catch (e: Exception) {
            log.error("Error parsing message: {}", rawPayload)
            return
        }

        // Handle the message
        when (eventMessage.type) {
            ManagerEvent.Type.PING -> session.send(ManagerEvent.pong)
            ManagerEvent.Type.PONG -> { }
            ManagerEvent.Type.HEARTBEAT -> {
                heartBeats[session.id] = LocalDateTime.now()
                session.send(ManagerEvent.heartbeat)
            }
            ManagerEvent.Type.REGISTER_FOR_EVENTS -> {
                observers[session.id] = session
                heartBeats[session.id] = LocalDateTime.now()
            }
            else -> {
                val msg = "Unexpected message type: ${eventMessage.type}\nMessage: $rawPayload"
                log.error(msg)
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        observers.remove(session.id)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ManagerEventsHandler::class.java)
    }
}