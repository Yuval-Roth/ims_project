package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameAction.Type
import com.imsproject.common.networking.UdpClient
import com.imsproject.gameServer.ClientHandler
import com.imsproject.gameServer.GameController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.SocketAddress


@Component
class UdpSocketHandler(private val gameController: GameController) {

    @Value("\${udp.local_port}")
    private var localPort : Int = 0


    private val clients = HashMap<String, ClientHandler>()
    private val enterCodes = HashMap<String, String>()
    private lateinit var socket : UdpClient

    fun send(message: String, address: SocketAddress){
        val (host,port) = address.toHostPort()
        socket.send(message, host,port)
    }

    fun addClient(client: ClientHandler, enterCode : String){
        enterCodes[enterCode] = client.id
        clients[client.id] = client
    }

    private fun run(){
        while(true) {
            val packet = socket.receiveRaw()
            val message = String(packet.data, 0, packet.length)
            handleMessage(message, packet.socketAddress)
        }
    }

    private fun handleMessage(message: String, address: SocketAddress){
        val action: GameAction
        try {
            action = GameAction.fromString(message)
        } catch (e: Exception) {
            log.debug("Error parsing message: $message\n${e.stackTraceToString()}")
            return
        }

        when (action.type) {
            Type.CLICK, Type.POSITION, Type.SYNC_TIME -> {
                val client = clients[address.toHostPortString()]
                if (client == null) {
                    log.debug("Client not found for packet from ${address.toHostPortString()}")
                    return
                }

                gameController.handleGameAction(client, action)
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
        socket = UdpClient().also{
            it.localPort = localPort
            it.init()
        }
        Thread(this::run).start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(UdpSocketHandler::class.java)
    }

}