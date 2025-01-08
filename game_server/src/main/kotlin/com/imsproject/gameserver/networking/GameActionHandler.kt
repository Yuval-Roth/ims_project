package com.imsproject.gameserver.networking

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameAction.Type
import com.imsproject.common.networking.UdpClient
import com.imsproject.gameserver.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap


@Component
class GameActionHandler(
    private val gameController: GameController,
    private val clients: ClientController
) {

    @Value("\${udp.local_port}")
    private var localPort : Int = 0

    private val enterCodes = ConcurrentHashMap<String, String>()
    private lateinit var socket : UdpClient

    fun send(message: String, address: SocketAddress){
        val (host,port) = address.toHostPort()
        socket.send(message, host,port)
    }

    fun addClient(client: ClientHandler, enterCode : String){
        enterCodes[enterCode] = client.id
    }

    private fun run(){
        while(true) {
            try{
                val packet = socket.receiveRaw()
                val message = String(packet.data, 0, packet.length)
                handleMessage(message, packet.socketAddress)
            } catch (e: Exception){
                log.error("An error occurred while handling a message:\n${e.stackTraceToString()}")
            }
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
                val client = clients.getByHostPort(address.toHostPortString())
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
                val clientId = enterCodes.remove(enterCode)
                if (clientId == null) {
                    log.debug("Enter code not found: $enterCode")
                    return
                }

                // get the client handler for the client id
                val clientHandler = clients.getByClientId(clientId)
                if (clientHandler == null) {
                    log.debug("Client not found for enter code: $enterCode")
                    return
                }
                clientHandler.udpAddress = address

                // map from address:port to the client
                clients.setHostPort(clientId, address.toHostPortString())

                // send confirmation to client
                send(GameAction.builder(Type.ENTER).build().toString(), address)
            }
        }
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
        private val log = LoggerFactory.getLogger(GameActionHandler::class.java)
    }

}