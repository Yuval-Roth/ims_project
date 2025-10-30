package com.imsproject.gameserver.business

import com.imsproject.gameserver.send
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.net.SocketAddress
import java.time.LocalDateTime
import java.util.concurrent.Semaphore

class ClientHandler internal constructor(
    val id: String,
    var wsSession: WebSocketSession,
    private val sendUdp: (String,SocketAddress) -> Unit
) {

    var lastHeartbeat: LocalDateTime = LocalDateTime.now()
    lateinit var udpAddress: SocketAddress
    var isConnected: Boolean = false
    private val wsLock = Semaphore(1,true)

    @Throws(IOException::class)
    fun sendTcp(message: String) {
        try {
            wsLock.acquire()
            if(wsSession.isOpen){
                try{
                    wsSession.send(message)
                } catch (e: IOException){
                    log.error("Error sending message to client: {}",e.stackTraceToString())
                }
            } else {
                log.debug("Attempted to send message to closed session: {}", message)
            }
        } finally {
            wsLock.release()
        }
    }

    @Throws(IOException::class)
    fun sendUdp(message: String) {
        val address = udpAddress
        sendUdp.invoke(message,address)
    }

    fun close() {
        wsSession.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ClientHandler::class.java)
    }
}