package com.imsproject.gameServer.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameServer.ClientController
import com.imsproject.gameServer.ClientHandler
import com.imsproject.gameServer.GameController
import com.imsproject.gameServer.send
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*

@Component
class WebSocketHandler(
    private val gameController: GameController,
    private val udpSocketHandler: UdpSocketHandler,
    private val clientController: ClientController
) : TextWebSocketHandler() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    }

    private val idGenerator: SimpleIdGenerator = SimpleIdGenerator(2)

    override fun afterConnectionEstablished(@NonNull session: WebSocketSession) {
        log.debug("New websocket client: {}", session.id)
    }

    override fun handleTextMessage(@NonNull session: WebSocketSession, message: TextMessage) {

        val rawPayload: String = message.payload

        // Parse the message
        val gameMessage: GameRequest
        try {
            gameMessage = GameRequest.fromJson(rawPayload)
        } catch (e: Exception) {
            log.error("Error parsing message: {}", rawPayload)
            return
        }

        // Handle the message
        when (gameMessage.type) {

            Type.PING -> session.send(GameRequest.pong())
            Type.PONG -> {}

            Type.ENTER -> {

                // Check if the client already exists
                if (clientController.containsByWsSessionId(session.id)) {
                    log.error("Client already exists for session: {}", session.id)
                    return
                }

                // create a new client handler for the session
                val client = newClientHandler(session)
                clientController.addClientHandler(session.id,client)

                log.debug("New client: {}",client.id)

                // generate code to add the client to the udp socket handler
                // the client will use this code to identify itself
                val udpCode = UUID.randomUUID().toString()
                udpSocketHandler.addClient(client, udpCode)

                // send the client id and the udp code to the client
                GameRequest.builder(Type.ENTER)
                    .playerId(client.id)
                    .data(listOf(udpCode))
                    .build()
                    .toJson()
                    .also { session.send(it) }
            }

            Type.EXIT -> session.close()

            else -> {
                // get the client handler for the session if it exists
                val client = clientController.getByWsSessionId(session.id) ?: run {
                    log.error("Client not found for session: {}", session.id)
                    return
                }
                try {
                    gameController.handleGameRequest(client, gameMessage)
                } catch (e: Exception) {
                    log.error("Error handling message", e)

                    // send an error message to the client
                    // to inform him he sent an invalid message
                    // if the error message is null, send a generic error message
                    client.sendTcp(
                        GameRequest.builder(Type.ERROR)
                            .message(e.message ?: "An error occurred")
                            .build().toJson())
                }
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, @NonNull status: CloseStatus) {
        val client = clientController.getByWsSessionId(session.id) ?: return
        log.debug("Client disconnected: {}", client.id)
        clientController.removeClientHandler(client.id)
    }

    private fun newClientHandler(wsSession: WebSocketSession) : ClientHandler {
        val id = idGenerator.generate()
        return ClientHandler(id, wsSession) { message, address, port ->
            udpSocketHandler.send(message, address, port)
        }
    }
}



