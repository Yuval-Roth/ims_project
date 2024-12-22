package com.imsproject.gameServer

import org.springframework.stereotype.Component
import java.time.LocalDateTime

private const val HEARTBEAT_TIMEOUT_THRESHOLD = 30L

@Component
class ClientController {

    private val clientIdToHandler = mutableMapOf<String, ClientHandler>()
    private val wsSessionIdToHandler = mutableMapOf<String, ClientHandler>()

    fun getByClientId(clientId: String): ClientHandler? {
        return clientIdToHandler[clientId]
    }

    fun getByWsSessionId(sessionId: String): ClientHandler? {
        return wsSessionIdToHandler[sessionId]
    }

    fun addClientHandler(sessionId: String, clientHandler: ClientHandler) {
        wsSessionIdToHandler[sessionId] = clientHandler
        clientIdToHandler[clientHandler.id] = clientHandler
    }

    fun removeClientHandler(clientId: String) {
        val handler = clientIdToHandler.remove(clientId) ?: return
        wsSessionIdToHandler.remove(handler.wsSessionId)
    }

    fun containsByClientId(clientId: String): Boolean {
        return clientIdToHandler.containsKey(clientId)
    }

    fun containsByWsSessionId(sessionId: String): Boolean {
        return wsSessionIdToHandler.containsKey(sessionId)
    }

    fun getAllClientIds(): List<String> {
         val iter = clientIdToHandler.iterator()
        while(iter.hasNext()){
            val entry = iter.next()
            val isAlive = entry.value
                .lastHeartbeat.plusSeconds(HEARTBEAT_TIMEOUT_THRESHOLD)
                .isAfter(LocalDateTime.now())
            if(!isAlive){
                iter.remove()
                wsSessionIdToHandler.remove(entry.value.wsSessionId)
            }
        }
        return clientIdToHandler.keys.toList()
    }
}