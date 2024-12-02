package com.imsproject.common.networking

import kotlinx.coroutines.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A simple UDP client that can send and receive messages to one remote address.
 *
 * A couple of things to keep in mind when using this class:
 * 1. You must call the [init] method to initialize the client before using it.
 * 2. To manually receive messages, call the [receive] method.
 * Otherwise, set the [onMessage] property and call the [startListener] method.
 * In this case, you should also set the [onListenerException] property.
 * Otherwise, any exception will be thrown and the listener thread will die.
 * 3. Once a listener is started, you can't call the [receive] or [setTimeout] methods.
 * 4. To stop the listener, call the [reset] method.
 */
class UdpClient {

    private var client: DatagramSocket? = null
    private var listener: Thread? = null
    private var listenerRunning : Boolean = false
    private val executor : ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * remote address that will be used in [send]
     */
    var remoteAddress: String?
        get() {
            return _remoteAddress?.hostAddress
        }
        set(value) {
            _remoteAddress = InetAddress.getByName(value)
        }
    private var _remoteAddress: InetAddress? = null

    /**
     * remote port that will be used in [send]
     */
    var remotePort: Int? = null

    /**
     * sets the local port for the client.
     *
     * If not set, the client will bind to a random local port.
     */
    var localPort: Int? = null

    /**
     * Setting this property will start a listener that will call the lambda every time
     * a message is received. Can't use the [receive] and [setTimeout] methods
     * while this property is set.
     */
    var onMessage : ((String) -> Unit) = {}
        set(value) {
            if(listenerRunning){
                throw IllegalStateException("This property can't be set after the listener has been started.")
            }
            field = value
        }

    /**
     * This lambda will be called every time an exception occurs on the listener thread.
     * If not set, the exception will be thrown and the listener thread will die.
     */
    var onListenerException : ((Exception) -> Unit)? = null

    @Throws(IOException::class)
    fun init (){

        // assert that the client is not already initialized
        if(client != null){
            throw IllegalStateException("Client is already initialized.")
        }

        val port = localPort
        val newClient = if(port == null){
            DatagramSocket()
        } else {
            DatagramSocket(port)
        }
        client = newClient
        localPort = newClient.localPort
    }

    /**
     * sends a UDP packet to the specified remoteAddress and remotePort
     */
    @Throws(IOException::class)
    fun send(message: String?, remoteAddress: String, remotePort: Int){
        // validate correct state
        val client = assertClientInitialized()

        // send the message
        val address = InetAddress.getByName(remoteAddress)
        val data = message?.toByteArray() ?: ByteArray(0)
        val packet = DatagramPacket(data, data.size,address, remotePort)
        client.send(packet)
    }

    /**
     * sends a UDP packet to [remoteAddress] : [remotePort]
     * @throws IllegalStateException if [remoteAddress] or [remotePort] are not set
     */
    @Throws(IOException::class)
    fun send(message: String?) {

        // validate correct state
        val client = assertClientInitialized()
        val address = _remoteAddress ?: throw IllegalStateException("Remote address is not set.")
        val port = remotePort ?: throw IllegalStateException("Remote port is not set.")

        // send the message
        val data = message?.toByteArray() ?: ByteArray(0)
        val packet = DatagramPacket(data, data.size, address, port)
        client.send(packet)
    }

    /**
     * @throws IOException if an I/O error occurs
     * @throws SocketTimeoutException if the timeout expires
     */
    @Throws(SocketTimeoutException::class, IOException::class)
    fun receive() : String {
        val client = assertClientInitialized()
        assertListenerNotRunning()
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        client.receive(packet)
        return String(packet.data, 0, packet.length)
    }

    /**
     * @return the raw [DatagramPacket] received
     * @throws IOException if an I/O error occurs
     * @throws SocketTimeoutException if the timeout expires
     */
    @Throws(SocketTimeoutException::class, IOException::class)
    fun receiveRaw() : DatagramPacket {
        val client = assertClientInitialized()
        assertListenerNotRunning()
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        client.receive(packet)
        return packet
    }

    /**
     * Sets the timeout for the client. If set to 0, the client will
     * block until a message is received. If set to a positive value,
     * the client will throw a [SocketTimeoutException] if no message
     * is received after the specified time.
     */
    fun setTimeout(timeout: Int){
        val client = assertClientInitialized()
        if(listenerRunning) {
            throw IllegalStateException("Listener is running. Cannot set timeout manually.")
        }
        client.soTimeout = timeout
    }

    /**
     * Starts a listener that will call the [onMessage] lambda every time a message is received.
     */
    fun startListener(){
        val client = assertClientInitialized()
        assertListenerNotRunning()
        listenerRunning = true
        client.soTimeout = 0
        val listener = Thread {
            while(true){
                try{
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)
                    client.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    executor.submit{onMessage(msg)}
                } catch (e: Exception){
                    if(listenerRunning.not()){
                        break
                    }
                    onListenerException?.run {
                        executor.submit{invoke(e)}
                    } ?: throw e
                }
            }
        }.apply{start()}

        this.listener = listener
    }

    /**
     * This method performs a controlled reset of the client, stopping the listener if it's running and closing the socket.
     * The client will be reinitialized with the same local port.
     * [onMessage] and [onListenerException] will be reset to their default values.
     */
    fun reset(){
        val oldClient = assertClientInitialized()
        listenerRunning = false
        listener = null
        client = null
        onMessage = {}
        onListenerException = null
        oldClient.close()
        init()
    }

    fun close(){
        val client = assertClientInitialized()
        client.close()
    }

    fun isReady() : Boolean {
        return client != null
                && client?.isClosed == false
    }

    private fun assertListenerNotRunning() {
        if (listenerRunning) {
            throw IllegalStateException("Cant call this method while the listener is running.")
        }
    }

    private fun assertClientInitialized() : DatagramSocket {
        if (client != null) {
            return client!!
        } else {
            throw IllegalStateException("Client is not initialized.")
        }
    }
}