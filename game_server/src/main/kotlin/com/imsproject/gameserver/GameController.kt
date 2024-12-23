package com.imsproject.gameserver

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.gameServer.GameType
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.lobbies.Lobby
import com.imsproject.gameserver.lobbies.LobbyState
import com.imsproject.gameserver.networking.ManagerEventsHandler
import com.imsproject.gameserver.networking.TimeServerHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class GameController(
        private val clientController: ClientController,
        private val timeServerHandler: TimeServerHandler,
        private val managerEventsHandler: ManagerEventsHandler
    ) {

    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val games = ConcurrentHashMap<String, Game>()
    private val clientIdToGame = ConcurrentHashMap<String, Game>()
    private val clientToLobby = ConcurrentHashMap<String, String>()
    private val idGenerator = SimpleIdGenerator(2)

    /**
     * Handles a game request from the manager
     */
    fun handleGameRequest(request: GameRequest) : String {
        try{
            return when(request.type){
                Type.GET_ONLINE_PLAYER_IDS -> handleGetOnlinePlayerIds()
                Type.GET_ALL_LOBBIES -> handleGetAllLobbies()
                Type.GET_LOBBY -> handleGetLobby(request)
                Type.CREATE_LOBBY -> handleCreateLobby(request)
                Type.REMOVE_LOBBY -> handleRemoveLobby(request)
                Type.SET_LOBBY_TYPE -> handleSetLobbyType(request)
                Type.JOIN_LOBBY -> handleJoinLobby(request)
                Type.LEAVE_LOBBY -> handleLeaveLobby(request)
                Type.START_GAME -> handleStartGame(request)
                Type.END_GAME -> handleEndGame(request)
                else -> Response.getError("Invalid message type")
            }
        } catch(e: Exception){
            log.error("Error handling game request", e)
            return Response.getError(e)
        }
    }

    private fun handleGetOnlinePlayerIds(): String {
        val playerIds = clientController.getAllClientIds()
        return Response.getOk(playerIds)
    }

    /**
     * Handles a game request from a client
     * @throws IllegalArgumentException if anything is wrong with the message
     */
    @Throws(IllegalArgumentException::class)
    fun handleGameRequest(clientHandler: ClientHandler, request: GameRequest) {
        when (request.type) {
            Type.TOGGLE_READY -> handleToggleReady(clientHandler)
            else -> throw IllegalArgumentException("Invalid message type: ${request.type}")
        }
    }

    /**
     * @throws IllegalArgumentException if anything is wrong with the message
     */
    @Throws(IllegalArgumentException::class)
    fun handleGameAction(clientHandler: ClientHandler, action: GameAction) {
        // ========= parameter validation ========= |
        val game = clientIdToGame[clientHandler.id] ?: run{
            log.debug("handleGameAction: Game not found for client: {}",clientHandler.id)
            throw IllegalArgumentException("Client not in game")
        }
        // ======================================== |

        game.handleGameAction(clientHandler, action)
    }

    private fun handleToggleReady(clientHandler: ClientHandler) {
        log.debug("handleToggleReady() with clientId: {}",clientHandler.id)

        // ========= parameter validation ========= |
        val lobbyId = clientToLobby[clientHandler.id] ?: run {
            log.debug("handleToggleReady: Player not in lobby")
            throw IllegalArgumentException("Player not in lobby")
        }
        val lobby = lobbies[lobbyId] ?: run {
            // should not happen
            log.error("handleToggleReady: lobbyId found for client by Lobby not found. client: {}",clientHandler.id)
            throw IllegalArgumentException("Lobby not found")
        }
        // ======================================== |

        val success = lobby.toggleReady(clientHandler.id)
        if (success) {
            // notify the manager
            val event = ManagerEvent.builder(ManagerEvent.Type.PLAYER_READY_TOGGLE)
                .playerId(clientHandler.id)
                .lobbyId(lobbyId)
                .build()
            managerEventsHandler.notify(event)
            log.debug("handleToggleReady() successful")
        } else {
            // should not happen
            log.error("handleToggleReady() failed: Lobby found for player but toggle ready failed")
            throw IllegalArgumentException("Toggle ready failed")
        }
    }

    private fun handleGetAllLobbies() : String {
        log.debug("handleGetAllLobbies()")

        val lobbiesInfo = lobbies.values.map { it.getInfo().toJson() }
        return Response.getOk(lobbiesInfo)
    }

    private fun handleGetLobby(request: GameRequest) : String {
        log.debug("handleGetLobby() with lobbyId: {}",request.lobbyId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: lobbyId"
        val lobbyId = request.lobbyId ?: run {
            log.debug("handleGetLobby: $errorMsg")
            return Response.getError(errorMsg)
        }
        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("handleGetLobby: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        return Response.getOk(lobby.getInfo().toJson())
    }

    private fun handleLeaveLobby(request: GameRequest): String {
        log.debug("handleLeaveLobby() with lobbyId: {}, playerId: {}",request.lobbyId,request.playerId)
        
        // ========= parameter validation ========= |
        val missingParams = mutableListOf<String>()
        val errorMsg = "Missing the following parameters: "
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val clientId = request.playerId ?: run { missingParams.add("playerId") ; null }
        if(lobbyId == null || clientId == null){
            log.debug("handleLeaveLobby: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby and player exist === |
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run { notFound.add("Lobby") ; null }
        /*(2)*/ val clientHandler = clientController.getByClientId(clientId) ?: run { notFound.add("Player") ; null }
        if(lobby == null || clientHandler == null){
            log.debug("handleLeaveLobby: {} {}",errorMsg2, notFound.joinToString())
            return Response.getError("$errorMsg2 ${notFound.joinToString()}")
        }
        // ======================================== |

        val success = lobby.remove(clientId)
        return if(success){
            clientToLobby.remove(clientId)
            // notify the client
            clientHandler.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            log.debug("handleLeaveLobby() successful")
            Response.getOk()
        } else {
            log.debug("handleLeaveLobby() failed: Player not in lobby")
            Response.getError("Player not in lobby")
        }
    }

    private fun handleJoinLobby(request: GameRequest) : String {
        log.debug("handleJoinLobby() with lobbyId: {}, playerId: {}",request.lobbyId,request.playerId)

        // ========= parameter validation ========= |
        val missingParams = mutableListOf<String>()
        val errorMsg = "Missing the following parameters: "
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val clientId = request.playerId ?: run { missingParams.add("playerId") ; null }
        if(lobbyId == null || clientId == null){
            log.debug("handleJoinLobby: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby and player exist === |
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run { notFound.add("Lobby") ; null }
        /*(2)*/ val clientHandler = clientController.getByClientId(clientId) ?: run { notFound.add("Player") ; null }
        if(lobby == null || clientHandler == null){
            log.debug("handleJoinLobby: {} {}",errorMsg2, notFound.joinToString())
            return Response.getError("$errorMsg2 ${notFound.joinToString()}")
        }
        // ======================================== |

        // Try to add the player to the lobby
        val success = lobby.add(clientId)

        return if(success){
            clientToLobby[clientId] = lobbyId
            // notify the client
            clientHandler.sendTcp(
                GameRequest.builder(Type.JOIN_LOBBY)
                    .lobbyId(lobbyId)
                    .gameType(lobby.gameType)
                    .build().toJson())
            log.debug("handleJoinLobby() successful")
            Response.getOk()
        } else {
            log.debug("handleJoinLobby() failed: Lobby is full")
            Response.getError("Lobby is full")
        }
    }

    private fun handleCreateLobby(request: GameRequest) : String {
        log.debug("handleCreateLobby() with gameType: {}", request.gameType)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: gameType"
        val gameType = request.gameType ?: run {
            log.debug("handleCreateLobby: {}",errorMsg)
            return Response.getError(errorMsg)
        }
        // ======================================== |

        val lobbyId = idGenerator.generate()
        val lobby = Lobby(lobbyId,gameType)
        lobbies[lobbyId] = lobby
        log.debug("handleCreateLobby() successful")
        return Response.getOk(lobbyId)
    }

    private fun handleRemoveLobby(request: GameRequest) : String {
        log.debug("handleRemoveLobby() with lobbyId: {}", request.lobbyId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: lobbyId"
        val lobbyId = request.lobbyId ?: run {
            log.debug("handleRemoveLobby: {}",errorMsg)
            return Response.getError(errorMsg)
        }
        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("handleRemoveLobby: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        lobbies.remove(lobbyId)
        // Notify the clients
        lobby.getPlayers()
            .map {clientController.getByClientId(it)}
            .forEach {
                it?.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            }

        log.debug("handleRemoveLobby() successful")
        return Response.getOk(lobbyId)
    }

    private fun handleEndGame(request: GameRequest) : String {
        log.debug("handleEndGame() with lobbyId: {}",request.lobbyId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: lobbyId"
        val lobbyId = request.lobbyId ?: run {
            log.debug("handleEndGame: {}",errorMsg)
            return Response.getError(errorMsg)
        }
        // === check if the lobby and game exist === |
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run { notFound.add("Lobby") ; null }
        /*(2)*/ val game = games[lobbyId] ?: run { notFound.add("Game") ; null }
        if(lobby == null || game == null){
            log.debug("handleEndGame: {} {}",errorMsg2, notFound.joinToString())
            return Response.getError("$errorMsg2 ${notFound.joinToString()}")
        }
        // ======================================== |

        game.endGame() // game.endGame() notifies the clients
        games.remove(lobby.id)
        lobby.state = LobbyState.WAITING

        // Notify the manager
        val event = ManagerEvent.builder(ManagerEvent.Type.GAME_ENDED)
            .lobbyId(lobbyId)
            .build()
        managerEventsHandler.notify(event)
        log.debug("handleEndGame() successful")
        return Response.getOk()
    }

    private fun handleStartGame(request: GameRequest) : String {
        log.debug("handleStartGame() with lobbyId: {}",request.lobbyId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: lobbyId"
        val lobbyId = request.lobbyId ?: run {
            log.debug("handleStartGame: {}",errorMsg)
            return Response.getError(errorMsg)
        }
        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("handleStartGame: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        // Check if the lobby is ready
        if(! lobby.isReady()) {
            log.debug("handleStartGame: Lobby is not ready")
            return Response.getError("Lobby is not ready")
        }

        // lobby is ready implies lobby has 2 players
        val player1Id = lobby.player1Id!!
        val player2Id = lobby.player2Id!!

        // ========= player validation ========= |
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val player1Handler = clientController.getByClientId(player1Id) ?: run{ notFound.add("Player 1 handler") ; null }
        /*(2)*/ val player2Handler = clientController.getByClientId(player2Id) ?: run{ notFound.add("Player 2 handler") ; null }
        if(player1Handler == null || player2Handler == null){
            // should not happen
            log.error("handleStartGame: {} {}",errorMsg2, notFound.joinToString())
            return Response.getError("Failed to start game")
        }
        // ======================================== |

        val game = when(lobby.gameType){
                GameType.WATER_RIPPLES -> {
                    log.debug("handleStartGame: Selected WaterRipplesGame")
                    WaterRipplesGame(player1Handler, player2Handler)
                }
                else -> {
                    log.debug("handleStartGame: Invalid game type")
                    return Response.getError("Invalid game type")
                }
        }
        lobby.state = LobbyState.PLAYING
        games[lobby.id] = game
        clientIdToGame[player1Id] = game
        clientIdToGame[player2Id] = game

        // game.startGame() notifies the clients
        game.startGame(timeServerHandler.timeServerCurrentTimeMillis())

        log.debug("handleStartGame() successful")
        return Response.getOk()
    }

    private fun handleSetLobbyType(request: GameRequest): String {
        log.debug("handleSetLobbyType() with lobbyId: {}, gameType: {}",request.lobbyId,request.gameType)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: "
        val missingParams = mutableListOf<String>()
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val gameType = request.gameType ?: run { missingParams.add("gameType") ; null }
        if(lobbyId == null || gameType == null){
            log.debug("handleSetLobbyType: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("handleSetLobbyType: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        lobby.gameType = gameType
        // Notify the clients
        lobby.getPlayers()
            .map {clientController.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                GameRequest.builder(Type.SET_LOBBY_TYPE)
                    .gameType(gameType)
                    .build().toJson()
            )
        }

        log.debug("handleSetLobbyType() successful")
        return Response.getOk()
    }

    companion object{
        private val log = LoggerFactory.getLogger(GameController::class.java)
    }
}
