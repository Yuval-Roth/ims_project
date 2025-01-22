package com.imsproject.gameserver.business

import com.imsproject.gameserver.send
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.net.SocketAddress
import java.time.LocalDateTime

class ClientHandler internal constructor(
    val id: String,
    var wsSession: WebSocketSession,
    private val sendUdp: (String,SocketAddress) -> Unit
) {

    var lastHeartbeat: LocalDateTime = LocalDateTime.now()
    lateinit var udpAddress: SocketAddress

    @Throws(IOException::class)
    fun sendTcp(message: String) {
        if(wsSession.isOpen){
            wsSession.send(message)
        } else {
            log.debug("Attempted to send message to closed session: {}", message)
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