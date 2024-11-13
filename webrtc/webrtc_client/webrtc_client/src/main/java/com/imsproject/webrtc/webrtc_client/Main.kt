package com.imsproject.webrtc.webrtc_client

import android.R.attr.end
import android.app.Application
import android.content.Context
import com.imsproject.utils.JsonUtils
import com.imsproject.utils.UdpClient
import com.imsproject.utils.WebSocketClient
import com.imsproject.utils.gameServer.GameMessage
import java.net.SocketTimeoutException
import java.net.URI

private const val HOST = "10.0.2.2"


class Main : Application() {

    private val context : Context = this

    private val ws = WebSocketClient(URI("ws://$HOST:8080/ws"))
    val udp = UdpClient(HOST, 4443)

    override fun onCreate() {
        super.onCreate()
        Thread {
            while(true){
                val start = System.nanoTime()
                udp.send("ping")
                try{
                    udp.receive()
                } catch(e: SocketTimeoutException){
                    continue
                }
                val end = System.nanoTime()
                println("UDP ping: ${(end - start).toDouble() / 1_000_000.0}ms")
                Thread.sleep(100)
            }
        }.start()

        Thread {
            ws.connectBlocking()
            val id = GameMessage.builder(GameMessage.Type.ENTER)
                .build()
                .toJson()
                .let {
                    ws.send(it)
                    ws.nextMessageBlocking().let {
                        JsonUtils.deserialize<GameMessage>(it, GameMessage::class.java).data
                    }
                }
            println("id: $id")
            val message = GameMessage.builder(GameMessage.Type.PING)
                .build()
                .toJson()
            while(true){
                val start = System.nanoTime()
                ws.send(message)
                ws.nextMessageBlocking()
                val end = System.nanoTime()
                println("TCP ping: ${(end - start).toDouble() / 1_000_000.0}ms")
                Thread.sleep(100)
            }
        }.start()
    }
}