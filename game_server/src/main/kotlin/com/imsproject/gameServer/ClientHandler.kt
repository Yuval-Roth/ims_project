package com.imsproject.gameServer

import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.net.SocketAddress

class ClientHandler internal constructor(
    val id: String,
    private val wsSession: WebSocketSession,
    private val sendUdp: (String,SocketAddress) -> Unit
) {

    var udpAddress: SocketAddress? = null

    @Throws(IOException::class)
    fun sendTcp(message: String) {
        wsSession.send(message)
    }

    @Throws(IOException::class)
    fun sendUdp(message: String) {
        val address = udpAddress?: throw IllegalStateException("UDP address is not set.")
        sendUdp.invoke(message,address)
    }
}