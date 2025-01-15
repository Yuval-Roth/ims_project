package com.imsproject.common.networking

import kotlinx.coroutines.*
import java.io.IOException
import java.net.*


private const val INTERRUPT_MESSAGE = "<!@#%^&*()INTERRUPT()*&^%#@!>"

class UdpClient {

    private var client: DatagramSocket? = null

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
    var localPort: Int = -1

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

        val newClient = if(localPort < 0){
            DatagramSocket()
        } else {
            DatagramSocket(localPort)
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
     * @throws InterruptedException if an interrupt message is received
     */
    @Throws(SocketTimeoutException::class, IOException::class, InterruptedException::class)
    fun receive() : String {
        val client = assertClientInitialized()
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        client.receive(packet)
        val message = String(packet.data, 0, packet.length)
        if(message == INTERRUPT_MESSAGE){
            throw InterruptedException("Received interrupt message")
        }
        return message
    }

    /**
     * @return the raw [DatagramPacket] received
     * @throws IOException if an I/O error occurs
     * @throws SocketTimeoutException if the timeout expires
     * @throws InterruptedException if an interrupt message is received
     */
    @Throws(SocketTimeoutException::class, IOException::class, InterruptedException::class)
    fun receiveRaw() : DatagramPacket {
        val client = assertClientInitialized()
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        client.receive(packet)
        if(packet.length == INTERRUPT_MESSAGE.length){
            val message = String(packet.data, 0, packet.length)
            if(message == INTERRUPT_MESSAGE){
                throw InterruptedException("Received interrupt message")
            }
        }
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
        client.soTimeout = timeout
    }

    fun interrupt(){
        send(INTERRUPT_MESSAGE, "127.0.0.1",localPort)
    }

    fun clearPendingMessages(): Int {
        val client = assertClientInitialized()
        val oldTimeout = client.soTimeout
        client.soTimeout = 100
        var clearedCount = 0
        while(true){
            try{
                receive()
                clearedCount++
            } catch (e: SocketTimeoutException){
                break
            }
        }
        client.soTimeout = oldTimeout
        return clearedCount
    }

    fun close(){
        val client = assertClientInitialized()
        client.close()
    }

    private fun assertClientInitialized() : DatagramSocket {
        return this.client ?: throw IllegalStateException("Client is not initialized.")
    }
}