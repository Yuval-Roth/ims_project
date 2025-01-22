package com.imsproject.gameserver.business

import com.imsproject.gameserver.isMoreThanSecondsAgo
import com.imsproject.gameserver.toHostPortString
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ClientController {

    companion object{
        const val HEARTBEAT_TIMEOUT_THRESHOLD = 60L
        private val log = LoggerFactory.getLogger(ClientController::class.java)
    }

    var onClientDisconnect: ((ClientHandler) -> Unit) = {}
        set(value) {
            _onClientDisconnect.add(value)
        }
    private val _onClientDisconnect = mutableListOf<((ClientHandler) -> Unit)>()

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

    fun addClientHandler(clientHandler: ClientHandler) {
        wsSessionIdToHandler[clientHandler.wsSession.id] = clientHandler
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
        wsSessionIdToHandler.remove(handler.wsSession.id)
        hostPortToHandler.remove(handler.udpAddress.toHostPortString())
    }

    fun containsByWsSessionId(sessionId: String): Boolean {
        return wsSessionIdToHandler.containsKey(sessionId)
    }

    fun getAllClientIds(): List<String> {
        return clientIdToHandler.keys.toList()
    }

    fun onExit(clientId: String){
        val handler = clientIdToHandler[clientId] ?: return
        removeClientHandler(clientId)
        _onClientDisconnect.forEach { it(handler) }
    }

    private fun pruneDeadClients() {
        val iter = clientIdToHandler.iterator()
        while(iter.hasNext()){
            val entry = iter.next()
            val handler = entry.value
            val isAlive = handler.lastHeartbeat.isMoreThanSecondsAgo(HEARTBEAT_TIMEOUT_THRESHOLD)
            if(!isAlive){
                iter.remove()
                wsSessionIdToHandler.remove(entry.value.wsSession.id)
                hostPortToHandler.remove(handler.udpAddress.toHostPortString())
                handler.close()
                _onClientDisconnect.forEach { it(handler) }
            }
        }
    }

    private fun run(){
        while(true){
            Thread.sleep(10000) // check every 10 seconds
            try{
                pruneDeadClients()
            } catch (e: Exception){
                log.debug(e.message,e)
            }
        }
    }

    @EventListener
    fun onApplicationReadyEvent(event: ApplicationReadyEvent){
        Thread(this::run).start()
    }
}