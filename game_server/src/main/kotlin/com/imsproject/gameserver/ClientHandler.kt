package com.imsproject.gameserver

import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.net.SocketAddress
import java.time.LocalDateTime

class ClientHandler internal constructor(
    val id: String,
    private val wsSession: WebSocketSession,
    private val sendUdp: (String,SocketAddress) -> Unit
) {

    val wsSessionId: String = wsSession.id
    var lastHeartbeat: LocalDateTime = LocalDateTime.now()
    lateinit var udpAddress: SocketAddress

    @Throws(IOException::class)
    fun sendTcp(message: String) {
        wsSession.send(message)
    }

    @Throws(IOException::class)
    fun sendUdp(message: String) {
        val address = udpAddress
        sendUdp.invoke(message,address)
    }

    fun close() {
        wsSession.close()
    }
}