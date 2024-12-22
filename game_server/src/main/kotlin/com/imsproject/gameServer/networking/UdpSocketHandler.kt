package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameAction.Type
import com.imsproject.common.networking.NonBlockingUdpClient
import com.imsproject.gameServer.ClientHandler
import com.imsproject.gameServer.GameController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors


@Component
class UdpSocketHandler(private val gameController: GameController) {

    @Value("\${udp.local_port}")
    private var localPort : Int = 0


    private val clients = HashMap<String, ClientHandler>()
    private val enterCodes = HashMap<String, String>()
    private lateinit var socket : NonBlockingUdpClient
    private val scope = CoroutineScope(Dispatchers.IO)
    private val executor = Executors.newFixedThreadPool(10)

    fun send(message: String, address: SocketAddress){
        val (host,port) = address.toHostPort()
        socket.sendBusyWait(message, host,port)
    }

    fun addClient(client: ClientHandler, enterCode : String){
        enterCodes[enterCode] = client.id
        clients[client.id] = client
    }

    private fun run(){

        val buffer = ByteBuffer.allocate(1024)

        while(true) {
            // try to receive a packet
            val senderAddress = socket.receiveNonBlocking(buffer) ?: continue
            println("received message ${System.currentTimeMillis()}")
            val timestamp = System.currentTimeMillis()
            buffer.flip()
            val message = String(buffer.array(), 0, buffer.limit())
            handleMessage(message, senderAddress, timestamp)
            println("handled message ${System.currentTimeMillis()}")
            buffer.clear()
        }
    }

    private fun handleMessage(message: String, address: SocketAddress, timestamp: Long){
        val action: GameAction
        try {
            action = GameAction.fromString(message)
        } catch (e: Exception) {
            log.debug("Error parsing message: $message\n${e.stackTraceToString()}")
            return
        }

        when (action.type) {
            Type.CLICK, Type.POSITION -> {
                println("coming out $timestamp")
                val client = clients[address.toHostPortString()]
                if (client == null) {
                    log.debug("Client not found for packet from ${address.toHostPortString()}")
                    return
                }

                gameController.handleGameAction(client, action, timestamp)
            }
            Type.PING -> send(GameAction.pong, address)
            Type.PONG -> {}
            Type.HEARTBEAT -> send(GameAction.heartbeat, address)

            Type.ENTER -> {

                // validate enter code is provided
                val enterCode = action.data
                if (enterCode == null) {
                    log.debug("Enter code not provided for client: ${address.toHostPortString()}")
                    return
                }

                // get the client id from the enter code
                val clientId = enterCodes[enterCode]
                if (clientId == null) {
                    log.debug("Enter code not found: $enterCode")
                    return
                }

                // get the client handler for the client id
                val clientHandler = clients[clientId]
                if (clientHandler == null) {
                    log.debug("Client not found for enter code: $enterCode")
                    return
                }

                // replace the mapping of to the client handler
                // from the clientId to the address:port of the client
                clients.remove(clientId)
                clients[address.toHostPortString()] = clientHandler

                // set the client's remote address
                clientHandler.udpAddress = address

                // send confirmation to client
                send(GameAction.builder(Type.ENTER).build().toString(), address)

                // remove the enter code
                enterCodes.remove(enterCode)
            }
        }
    }

    private fun SocketAddress.toHostPortString() : String {
        val (host,port) = this.toHostPort()
        return "$host:$port"
    }

    private fun SocketAddress.toHostPort() : Pair<String,Int> {
        val cleanAddress = this.toString().substring(1)
        val parts = cleanAddress.split(":")
        return Pair(parts[0], parts[1].toInt())
    }

    @EventListener
    fun onApplicationReadyEvent(event: ApplicationReadyEvent){
        socket = NonBlockingUdpClient().also{
            it.localPort = localPort
            it.init()
        }
        Thread(this::run).start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(UdpSocketHandler::class.java)
    }

}