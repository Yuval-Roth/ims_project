package com.imsproject.gameServer

import org.springframework.web.socket.WebSocketSession
import java.io.IOException

class ClientHandler internal constructor(
    val id: String,
    private val wsSession: WebSocketSession,
    private val sendUdp: (String,String,Int) -> Unit
) {

    var udpRemoteAddress: String? = null
    var udpRemotePort: Int? = null

    @Throws(IOException::class)
    fun sendTcp(message: String) {
        wsSession.send(message)
    }

    @Throws(IOException::class)
    fun sendUdp(message: String) {
        val address = udpRemoteAddress ?: throw IllegalStateException("UDP address is not set")
        val port = udpRemotePort ?: throw IllegalStateException("UDP port is not set")
        sendUdp.invoke(message, address,port)
    }
}