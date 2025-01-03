package com.imsproject.gameserver.networking

import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.*
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
class GameRequestHandler(
    private val gameController: GameController,
    private val gameActionHandler: GameActionHandler,
    private val clientController: ClientController,
    private val managerEventsHandler: ManagerEventsHandler
) : TextWebSocketHandler() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GameRequestHandler::class.java)
    }

    private val idGenerator: SimpleIdGenerator = SimpleIdGenerator(3)

    override fun afterConnectionEstablished(@NonNull session: WebSocketSession) {
        log.debug("New game requests client connected with id: {}", session.id)
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
            Type.HEARTBEAT -> {
                clientController.getByWsSessionId(session.id)?.let {
                    it.lastHeartbeat = LocalDateTime.now()
                    session.send(GameRequest.heartbeat)
                }
            }

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
                gameActionHandler.addClient(client, udpCode)

                // send the client id and the udp code to the client
                GameRequest.builder(Type.ENTER)
                    .playerId(client.id)
                    .data(listOf(udpCode))
                    .build()
                    .toJson()
                    .also { session.send(it) }

                //notify the manager
                val event = ManagerEvent.builder(ManagerEvent.Type.PLAYER_CONNECTED)
                    .playerId(client.id)
                    .build()
                managerEventsHandler.notify(event)
            }

            Type.EXIT -> session.close()

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
                    gameController.handleGameRequest(client, gameMessage)
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
        clientController.removeClientHandler(client.id)
        gameController.onClientDisconnect(client)
        //notify the manager
        val event = ManagerEvent.builder(ManagerEvent.Type.PLAYER_DISCONNECTED)
            .playerId(client.id)
            .build()
        managerEventsHandler.notify(event)
    }

    private fun newClientHandler(wsSession: WebSocketSession) : ClientHandler {
        val id = idGenerator.generate()
        return ClientHandler(id, wsSession) { message, address ->
            gameActionHandler.send(message, address)
        }
    }
}



