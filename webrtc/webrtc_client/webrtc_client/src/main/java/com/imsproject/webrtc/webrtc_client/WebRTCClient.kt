package com.imsproject.webrtc.webrtc_client

import com.imsproject.utils.Response
import com.imsproject.utils.webrtc.WebRTCRequest
import java.net.URI
import java.util.concurrent.ConcurrentLinkedDeque

class WebRTCClient(private val serverIp: String, private val serverPort: Int) {

    private lateinit var ws : WebSocketClient
    private lateinit var clientId : String
    private val messagesQueue : ConcurrentLinkedDeque<String> = ConcurrentLinkedDeque()
    private var connected : Boolean = false

    fun connectToServer() : Boolean {
        if(connected){
            throw IllegalStateException("Already connected to signaling server")
        }

        ws = WebSocketClient(URI("ws://$serverIp:$serverPort"))
        ws.onMessageListener = { messagesQueue.add(it) }

        if (! ws.connectBlocking()){
            return false
        }

        val request = WebRTCRequest.builder()
            .setType(WebRTCRequest.Type.ENTER)
            .build()
            .toJson()
        ws.send(request)

        var response : String? = null
        do {
            response = messagesQueue.poll()
            Thread.sleep(10);
        } while (response == null)

        try{
            val responseObj = Response.fromJson(response)
            if (responseObj.success()){
                clientId = responseObj.payload<String>(String.javaClass).first()
                connected = true
                return true
            }
        } catch (ignored : Exception){
            // TODO: Handle this exception and log it
        }
        ws.close()
        return false
    }



}