package com.imsproject.webrtc.webrtc_client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class WebSocketClient (serverUri: URI) : WebSocketClient(serverUri) {

    var onOpenListener : (ServerHandshake?) -> Unit = {}
    var onMessageListener : (String?) -> Unit = {}
    var onCloseListener : (Int, String?, Boolean) -> Unit = {_, _, _ ->}
    var onErrorListener : (Exception?) -> Unit = {}


    override fun onOpen(handshakedata: ServerHandshake?) {
        onOpenListener.invoke(handshakedata)
    }

    override fun onMessage(message: String?) {
        onMessageListener.invoke(message)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        onCloseListener.invoke(code, reason, remote)
    }

    override fun onError(ex: Exception?) {
        onErrorListener.invoke(ex)
    }
}