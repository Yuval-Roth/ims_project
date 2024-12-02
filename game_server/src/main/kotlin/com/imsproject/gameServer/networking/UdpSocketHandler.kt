package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.networking.UdpClient
import com.imsproject.gameServer.ClientHandler
import com.imsproject.gameServer.GameController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.DatagramPacket


@Component
class UdpSocketHandler(private val gameController: GameController) {

    @Value("\${udp.local_port}")
    private var localPort : Int = 0

    private val clients = mutableMapOf<String, ClientHandler>()
    private val enterCodes = mutableMapOf<String, String>()
    private lateinit var socket : UdpClient

    fun send(message: String, remoteAddress: String, remotePort: Int){
        socket.send(message, remoteAddress, remotePort)
    }

    fun addClient(client: ClientHandler, enterCode : String){
        val id = "${client.udpRemoteAddress}:${client.udpRemotePort}"
        enterCodes[enterCode] = id
        clients[id] = client
    }

    private fun run(){
        socket = UdpClient().also{
            it.localPort = localPort
            it.init()
        }
        while(true) {
            val packet = socket.receiveRaw() // wait for a packet


            val message = String(packet.data, 0, packet.length)
            val action : GameAction
            try{
                action = GameAction.fromString(message)
            } catch (e: Exception){
                log.debug("Error parsing message: $message")
                continue
            }

            when(action.type){
                GameAction.Type.PING -> send(GameAction.pong(), packet.address.hostAddress, packet.port)
                GameAction.Type.PONG -> {}

                GameAction.Type.ENTER -> {

                    // validate enter code is provided
                    val enterCode = action.data
                    if (enterCode == null) {
                        log.debug("Enter code not provided for client: ${packet.address.hostAddress}:${packet.port}")
                        continue
                    }

                    // get the client id from the enter code
                    val clientId = enterCodes[enterCode]
                    if (clientId == null) {
                        log.debug("Enter code not found: $enterCode")
                        continue
                    }

                    // get the client handler for the client id
                    val clientHandler = clients[clientId]
                    if (clientHandler == null) {
                        log.debug("Client not found for enter code: $enterCode")
                        continue
                    }

                    // set the client's remote address and port
                    clientHandler.udpRemoteAddress = packet.address.hostAddress
                    clientHandler.udpRemotePort = packet.port

                    // send confirmation to client
                    send(
                        GameAction.builder(GameAction.Type.ENTER).build().toString()
                        ,packet.address.hostAddress, packet.port)

                    // remove the enter code
                    enterCodes.remove(enterCode)
                }
                else ->{
                    val client = packetToClient(packet)
                    if (client == null) {
                        log.debug("Client not found for packet from ${packet.address.hostAddress}:${packet.port}")
                        continue
                    }
                    gameController.handleGameAction(client, action)
                }
            }
        }
    }

    private fun packetToClient(packet: DatagramPacket) : ClientHandler? {
        return clients["${packet.address.hostAddress}:${packet.port}"]
    }

    @EventListener
    fun onApplicationReadyEvent(event: ApplicationReadyEvent){
        Thread(this::run).start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(UdpSocketHandler::class.java)
    }

}