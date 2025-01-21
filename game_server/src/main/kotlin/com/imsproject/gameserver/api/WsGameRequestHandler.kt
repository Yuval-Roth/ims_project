package com.imsproject.gameserver.api

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.*
import com.imsproject.gameserver.business.ClientController
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.GameRequestFacade
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.LocalDateTime
import java.util.*

@Component
class WsGameRequestHandler(
    private val facade: GameRequestFacade,
    private val gameActionHandler: UdpGameActionHandler,
    private val clientController: ClientController
) : TextWebSocketHandler() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WsGameRequestHandler::class.java)
    }

    private val idGenerator: SimpleIdGenerator = SimpleIdGenerator(3)

    override fun afterConnectionEstablished(@NonNull session: WebSocketSession) {
        log.debug("New websocket client connected with id: {}", session.id)
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

            Type.PING -> session.send(GameRequest.pong)
            Type.PONG -> {}
            Type.EXIT -> {
                clientController.getByWsSessionId(session.id)?.let {
                    clientController.removeClientHandler(it.id)
                    log.debug("Client disconnected: {}", it.id)
                }
            }
            Type.HEARTBEAT -> {
                clientController.getByWsSessionId(session.id)?.let {
                    it.lastHeartbeat = LocalDateTime.now()
                }
                session.send(GameRequest.heartbeat)
            }

            Type.ENTER_WITH_ID -> {
                val id = gameMessage.playerId ?: run {
                    log.error("No client id provided")
                    return
                }

                log.debug("New client: {}",id)

                // Check if the id is already connected from elsewhere
                // and if so, disconnect the old connection
                var client = clientController.getByClientId(id)
                if (client != null) {
                    log.debug("Client with id {} already connected, disconnecting old connection", id)
                    val msg = GameRequest.builder(Type.EXIT)
                        .message("Client with id $id connected from another location")
                        .build().toJson()
                    try{
                        client.sendTcp(msg) // send a message to the old client
                    } catch (_: Exception) { }

                    // map the client to the new wsSession
                    clientController.removeClientHandler(client.id) // clear old mappings
                    client.wsSession = session
                    clientController.addClientHandler(client)

                } else {
                    // client is new, create a new client handler
                    client = newClientHandler(session, id)
                    clientController.addClientHandler(client)
                }

                // generate code to add the client to the udp socket handler
                // the client will use this code to identify itself
                val udpCode = UUID.randomUUID().toString()
                gameActionHandler.addClient(client, udpCode)

                // send the udp code to the client
                GameRequest.builder(Type.ENTER_WITH_ID)
                    .data(listOf(udpCode))
                    .build()
                    .toJson()
                    .also { session.send(it) }
            }

            Type.ENTER -> {

                // Check if the client already exists
                if (clientController.containsByWsSessionId(session.id)) {
                    log.error("Client already exists for session: {}", session.id)
                    return
                }

                // create a new client handler for the session
                val client = newClientHandler(session)
                clientController.addClientHandler(client)

                log.debug("New client: {}",client.id)

                // generate code to add the client to the udp socket handler
                // the client will use this code to identify itself
                val udpCode = UUID.randomUUID().toString()
                gameActionHandler.addClient(client, udpCode)

                // send the client id and the udp code to the client
                GameRequest.builder(Type.ENTER)
                    .playerId(client.id)
                    .data(listOf(udpCode))
                    .build()
                    .toJson()
                    .also { session.send(it) }
            }

            Type.RECONNECT -> {
                val id = gameMessage.playerId ?: run {
                    log.error("No client id provided")
                    return
                }

                // Check that a client exists
                val client = clientController.getByClientId(id) ?: run {
                    log.error("Client not found for id: {}", id)
                    return
                }
                // map the client to the new wsSession
                clientController.removeClientHandler(client.id) // clear old mappings
                client.wsSession = session
                clientController.addClientHandler(client)
                client.lastHeartbeat = LocalDateTime.now()

                log.debug("Reconnected client: {}", client.id)

                // generate code to add the client to the udp socket handler
                // the client will use this code to identify itself
                val udpCode = UUID.randomUUID().toString()
                gameActionHandler.addClient(client, udpCode)

                // send the client id and the udp code to the client
                GameRequest.builder(Type.RECONNECT)
                    .data(listOf(udpCode))
                    .build()
                    .toJson()
                    .also { session.send(it) }

            }

            else -> {
                // get the client handler for the session if it exists
                val client = clientController.getByWsSessionId(session.id) ?: run {
                    log.error("Client not found for session: {}", session.id)
                    session.send(GameRequest.builder(Type.ERROR)
                            .message("Client not found, please reconnect")
                            .build().toJson())
                    return
                }
                try {
                    facade.handleGameRequest(client, gameMessage)
                } catch (e: Exception) {
                    log.error("Error handling message", e)

                    // send an error message to the client
                    // to inform him he sent an invalid message
                    // if the error message is null, send a generic error message
                    client.sendTcp(GameRequest.builder(Type.ERROR)
                            .message(e.message ?: "An error occurred:\n${e.stackTraceToString()}")
                            .data(listOf(rawPayload))
                            .build().toJson())
                }
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, @NonNull status: CloseStatus) {
        val client = clientController.getByWsSessionId(session.id) ?: return
        log.debug("Client disconnected: {}", client.id)
    }

    private fun newClientHandler(wsSession: WebSocketSession, selectedId: String? = null) : ClientHandler {
        val id = selectedId ?: idGenerator.generate()
        return ClientHandler(id, wsSession) { message, address ->
            gameActionHandler.send(message, address)
        }
    }
}



