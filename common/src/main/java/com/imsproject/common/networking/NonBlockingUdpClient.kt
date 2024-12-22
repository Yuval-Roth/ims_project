package com.imsproject.common.networking

import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * A simple UDP client that can send and receive messages to one remote address.
 *
 * A couple of things to keep in mind when using this class:
 * 1. You must call the [init] method to initialize the client before using it.
 * 2. To manually receive messages, call the [receiveNonBlocking] method.
 * Otherwise, set the [onMessage] property and call the [startListener] method.
 * In this case, you should also set the [onListenerException] property.
 * Otherwise, any exception will be thrown and the listener thread will die.
 * 3. Once a listener is started, you can't call the [receiveNonBlocking] or [setTimeout] methods.
 * 4. To stop the listener, call the [reset] method.
 */
class NonBlockingUdpClient {

    private var channel: DatagramChannel? = null

    /**
     * remote address that will be used in [sendNonBlocking]
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
     * remote port that will be used in [sendNonBlocking]
     */
    var remotePort: Int? = null

    /**
     * sets the local port for the client.
     *
     * If not set, the client will bind to a random local port.
     */
    var localPort: Int? = null

    @Throws(IOException::class)
    fun init (){

        val channel = DatagramChannel.open()

        // assert that the client is not already initialized
        if(this.channel != null){
            throw IllegalStateException("Client is already initialized.")
        }

        val port = localPort
        if(port == null){
            channel.bind(null)
        } else {
            channel.bind(InetSocketAddress(port))
        }
        channel.configureBlocking(false)

        this.channel = channel
        this.localPort = channel.localAddress.toString().split(":").last().toInt()
    }

    @Throws(IOException::class)
    fun sendBusyWait(message: String?, remoteAddress: String, remotePort: Int) {
        // validate correct state
        val channel = assertChannelInitialized()

        // send the message
        val sendAddress = InetSocketAddress(remoteAddress, remotePort)
        val buffer = ByteBuffer.wrap(message?.toByteArray() ?: ByteArray(0))
        var ans: Int
        do{
           ans = channel.send(buffer, sendAddress)
        } while (ans == 0)
    }

    /**
     * sends a UDP packet to the specified remoteAddress and remotePort
     * @return true if the message was sent successfully, false otherwise
     */
    @Throws(IOException::class)
    fun sendNonBlocking(message: String?, remoteAddress: String, remotePort: Int) : Boolean {
        // validate correct state
        val channel = assertChannelInitialized()

        // send the message
        val sendAddress = InetSocketAddress(remoteAddress, remotePort)
        val buffer = ByteBuffer.wrap(message?.toByteArray() ?: ByteArray(0))
        return channel.send(buffer, sendAddress) != 0
    }

    /**
     * sends a UDP packet to [remoteAddress] : [remotePort]
     * @throws IllegalStateException if [remoteAddress] or [remotePort] are not set
     * @return true if the message was sent successfully, false otherwise
     */
    @Throws(IOException::class)
    fun sendNonBlocking(message: String?) : Boolean {
        val address = _remoteAddress ?: throw IllegalStateException("Remote address is not set.")
        val port = remotePort ?: throw IllegalStateException("Remote port is not set.")
        return sendNonBlocking(message, address.hostAddress, port)
    }

    @Throws(IOException::class)
    fun receiveNonBlocking(buffer: ByteBuffer) : SocketAddress? {
        // validate correct state
        val channel = assertChannelInitialized()

        // receive the message
        return channel.receive(buffer)
    }

    fun close(){
        val channel = assertChannelInitialized()
        channel.close()
    }

    private fun assertChannelInitialized() : DatagramChannel {
        return this.channel ?: throw IllegalStateException("Channel is not initialized.")
    }
}