package com.imsproject.game_server

import org.springframework.stereotype.Component

@Component
class ClientController {

    private val clientIdToHandler = mutableMapOf<String, ClientHandler>()
    private val wsSessionIdToHandler = mutableMapOf<String, ClientHandler>()
    private val clientIdToWsSessionId = mutableMapOf<String, String>()

    fun getByClientId(clientId: String): ClientHandler? {
        return clientIdToHandler[clientId]
    }

    fun getByWsSessionId(sessionId: String): ClientHandler? {
        return wsSessionIdToHandler[sessionId]
    }

    fun addClientHandler(sessionId: String, clientHandler: ClientHandler) {
        wsSessionIdToHandler[sessionId] = clientHandler
        clientIdToHandler[clientHandler.id] = clientHandler
        clientIdToWsSessionId[clientHandler.id] = sessionId
    }

    fun removeClientHandler(clientId: String) {
        clientIdToHandler.remove(clientId)
        val sessionId = clientIdToWsSessionId.remove(clientId)
        if (sessionId != null) {
            wsSessionIdToHandler.remove(sessionId)
        }
    }

    fun containsByClientId(clientId: String): Boolean {
        return clientIdToHandler.containsKey(clientId)
    }

    fun containsByWsSessionId(sessionId: String): Boolean {
        return wsSessionIdToHandler.containsKey(sessionId)
    }
}