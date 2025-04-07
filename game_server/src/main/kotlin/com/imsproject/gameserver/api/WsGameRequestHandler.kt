package com.imsproject.gameserver.api

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.*
import com.imsproject.gameserver.business.ClientController
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.GameRequestFacade
import com.imsproject.gameserver.business.LobbyController
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.dataAccess.implementations.ParticipantPK
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
    private val clientController: ClientController,
    private val lobbyController: LobbyController,
    private val daoController: DAOController
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
            gameMessage = fromJson(rawPayload)
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
                    clientController.onExit(it.id)
                    log.debug("Client exited: {}", it.id)
                }
            }
            Type.HEARTBEAT -> {
                clientController.getByWsSessionId(session.id)?.let {
                    it.lastHeartbeat = LocalDateTime.now()
                }
                session.send(GameRequest.heartbeat)
            }

            Type.ENTER -> handleEnter(gameMessage, session,false)
            Type.FORCE_ENTER -> handleEnter(gameMessage, session,true)

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

    private fun handleEnter(
        gameMessage: GameRequest,
        session: WebSocketSession,
        force: Boolean
    ) {
        val id = gameMessage.playerId ?: run {
            log.error("No client id provided")
            return
        }

        // check if the participant exists in the database
        val pk = ParticipantPK(
            try{
                id.toInt()
            } catch (e: Exception){
                log.error("Invalid participant id")
                return
            }
        )
        if(! daoController.handleExists(pk)){
            log.debug("Participant with id {} not found", id)
            session.send(GameRequest.builder(Type.PARTICIPANT_NOT_FOUND).build().toJson())
            return
        }

        log.debug("New client: {}", id)

        // Check if the id is already connected from elsewhere
        // and if so, disconnect the old connection
        var clientHandler = clientController.getByClientId(id)
        if (clientHandler != null) {
            if(clientHandler.isConnected){
                log.debug("Client with id {} already connected", id)
                if(force){
                    log.debug("Forcing client with id {} to connect from a different location", id)
                    val msg = GameRequest.builder(Type.EXIT)
                        .message("Client with id $id connected from another location")
                        .build().toJson()
                    try {
                        clientHandler.sendTcp(msg) // send a message to the old client
                    } catch (_: Exception) {
                    }
                } else {
                    log.debug("Informing client with id {} of the conflict", id)
                    session.send(GameRequest.builder(Type.ALREADY_CONNECTED)
                        .build().toJson())
                    return
                }
            }

            // map the client to the new wsSession
            clientController.removeClientHandler(clientHandler.id) // clear old mappings
            clientHandler.wsSession = session
            clientController.addClientHandler(clientHandler)
        } else {
            // client is new, create a new client handler
            clientHandler = newClientHandler(session, id)
            clientController.addClientHandler(clientHandler)
        }

        clientHandler.isConnected = true

        // generate code to add the client to the udp socket handler
        // the client will use this code to identify itself
        val udpCode = UUID.randomUUID().toString()
        gameActionHandler.addClient(clientHandler, udpCode)

        // send the udp code to the client
        GameRequest.builder(if(force) Type.FORCE_ENTER else Type.ENTER)
            .data(listOf(udpCode))
            .build()
            .toJson()
            .also { session.send(it) }

        if (lobbyController.isClientInALobby(id)) {
            lobbyController.onClientConnect(clientHandler)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, @NonNull status: CloseStatus) {
        val client = clientController.getByWsSessionId(session.id) ?: return
        client.isConnected = false
        log.debug("afterConnectionClosed: client {} websocket session disconnected", client.id)
    }

    private fun newClientHandler(wsSession: WebSocketSession, selectedId: String? = null) : ClientHandler {
        val id = selectedId ?: idGenerator.generate()
        return ClientHandler(id, wsSession) { message, address ->
            gameActionHandler.send(message, address)
        }
    }
}



