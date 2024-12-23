package com.imsproject.gameserver

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ClientController {

    private val clientIdToHandler = ConcurrentHashMap<String, ClientHandler>()
    private val wsSessionIdToHandler = ConcurrentHashMap<String, ClientHandler>()
    private val hostPortToHandler = ConcurrentHashMap<String, ClientHandler>()

    fun getByClientId(clientId: String): ClientHandler? {
        return clientIdToHandler[clientId]
    }

    fun getByWsSessionId(sessionId: String): ClientHandler? {
        return wsSessionIdToHandler[sessionId]
    }

    fun getByHostPort(hostPort: String): ClientHandler? {
        return hostPortToHandler[hostPort]
    }

    fun addClientHandler(sessionId: String, clientHandler: ClientHandler) {
        wsSessionIdToHandler[sessionId] = clientHandler
        clientIdToHandler[clientHandler.id] = clientHandler
    }

    /**
     * @throws IllegalStateException if no client handler is found for the given client id
     */
    @Throws (IllegalStateException::class)
    fun setHostPort(clientId: String, hostPort: String) {
        val handler = clientIdToHandler[clientId] ?: throw IllegalStateException("No client handler found for client id $clientId")
        hostPortToHandler[hostPort] = handler
    }

    fun removeClientHandler(clientId: String) {
        val handler = clientIdToHandler.remove(clientId) ?: return
        wsSessionIdToHandler.remove(handler.wsSessionId)
    }

    fun containsByWsSessionId(sessionId: String): Boolean {
        return wsSessionIdToHandler.containsKey(sessionId)
    }

    fun getAllClientIds(): List<String> {
        val iter = clientIdToHandler.iterator()
        while(iter.hasNext()){
            val entry = iter.next()
            val isAlive = entry.value
                .lastHeartbeat.isMoreThanSecondsAgo(HEARTBEAT_TIMEOUT_THRESHOLD)
            if(!isAlive){
                iter.remove()
                wsSessionIdToHandler.remove(entry.value.wsSessionId)
            }
        }
        return clientIdToHandler.keys.toList()
    }
}