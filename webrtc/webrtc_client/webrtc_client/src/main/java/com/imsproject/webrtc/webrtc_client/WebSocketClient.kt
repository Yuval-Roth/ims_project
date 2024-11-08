package com.imsproject.webrtc.webrtc_client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * A simple WebSocketClient that can be used to connect to a WebSocket server
 * and send and receive messages
 * By default, the client automatically saves the messages received from the server in a queue
 * that can be accessed using the [nextMessage] or [nextMessageBlocking] methods
 */
class WebSocketClient (serverUri: URI) : WebSocketClient(serverUri) {

    private val messagesQueue : MutableList<String> = mutableListOf()
    private var isOverridden : Boolean = false
    private var interrupted : Boolean = false
    private val lock = Object()

    // TODO: check if this is necessary
    // used to prevent blocking of the calling thread when actions are received
    private val executor : ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * This listener will be called when a message is received from the server
     * By default, it will take the message and store it in a queue that can be accessed by
     * the [nextMessage] or [nextMessageBlocking] methods.
     * NOTE: overriding this listener will allow you to handle the message in a different way
     * but will not allow you to access the message using the [nextMessage] or [nextMessageBlocking] methods
     */
    var onMessageListener : (String?) -> Unit = {
        if(it != null){
            synchronized(lock){
                messagesQueue.add(it)
                lock.notifyAll()
            }
        }
    }
        set(value){
            field = value
            isOverridden = true
        }

    var onOpenListener : (ServerHandshake?) -> Unit = {}
    var onCloseListener : (Int, String?, Boolean) -> Unit = {_, _, _ ->}
    var onErrorListener : (Exception?) -> Unit = {}


    fun hasMessages() : Boolean {
        checkIfOverridden()

        synchronized(lock){
            return messagesQueue.isNotEmpty()
        }
    }

    /**
     * interrupt the blocking [nextMessageBlocking] method
     */
    fun interrupt(){
        checkIfOverridden()

        synchronized(lock){
            interrupted = true
            lock.notifyAll()
        }
    }

    /**
     * Get the next message in the queue non-blocking
     */
    fun nextMessage() : String? {
        checkIfOverridden()

        synchronized(lock){
            return if(messagesQueue.isNotEmpty()){
                messagesQueue.first();
            } else {
                null
            }
        }
    }

    /**
     * This method will block until a message is received from the server
     * or until the [interrupt] method is called
     * @throws InterruptedException if the method is interrupted
     */
    fun nextMessageBlocking() : String {
        checkIfOverridden()

        var message : String? = null
        synchronized(lock){
            while(! interrupted && message == null){
                if(messagesQueue.isNotEmpty()){
                    message = messagesQueue.removeAt(0)
                } else {
                    lock.wait()
                }
            }
        }

        if(interrupted){
            interrupted = false
            throw InterruptedException("nextMessageBlocking was interrupted")
        }

        if(message == null){
            throw IllegalStateException("Should not happen")
        }

        return message
    }

    private fun checkIfOverridden() {
        if (isOverridden) {
            throw IllegalStateException("Cannot access messages queue when onMessageListener is overridden")
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        executor.submit { onOpenListener.invoke(handshakedata) }
    }

    override fun onMessage(message: String?) {
        executor.submit { onMessageListener.invoke(message) }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        executor.submit { onCloseListener.invoke(code, reason, remote) }
    }

    override fun onError(ex: Exception?) {
        executor.submit { onErrorListener.invoke(ex) }
    }
}