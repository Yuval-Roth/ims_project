package com.imsproject.gameserver

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.games.FlourMillGame
import com.imsproject.gameserver.games.Game
import com.imsproject.gameserver.games.WaterRipplesGame
import com.imsproject.gameserver.games.WineGlassesGame
import com.imsproject.gameserver.lobbies.Lobby
import com.imsproject.gameserver.lobbies.LobbyState
import com.imsproject.gameserver.networking.TimeServerHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListMap

@Component
class GameController(
        private val clientController: ClientController,
        private val timeServerHandler: TimeServerHandler
    ) {

    init{
        clientController.onClientDisconnect = { onClientDisconnect(it) }
    }

    private data class Session (
        val sessionId: String,
        val duration: Int,
        val gameType: GameType
    )

    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val games = ConcurrentHashMap<String, Game>()
    private val clientIdToGame = ConcurrentHashMap<String, Game>()
    private val clientIdToLobbyId = ConcurrentHashMap<String, String>()
    private val lobbyIdGenerator = SimpleIdGenerator(4)
    private val lobbyIdToSessions = ConcurrentHashMap<String, ConcurrentLinkedDeque<Session>>()
    private val sessionIdGenerator = SimpleIdGenerator(5)

    // ========================================================================== |
    // ========================= PUBLIC METHODS ================================= |
    // ========================================================================== |

    /**
     * Handles a game request from the manager
     */
    fun handleGameRequest(request: GameRequest) : String {
        try {
            return when (request.type) {
                Type.GET_ONLINE_PLAYER_IDS -> handleGetOnlinePlayerIds()
                Type.GET_ALL_LOBBIES -> handleGetAllLobbies()
                Type.GET_LOBBY -> handleGetLobby(request)
                Type.CREATE_LOBBY -> handleCreateLobby(request)
                Type.REMOVE_LOBBY -> handleRemoveLobby(request)
                Type.SET_LOBBY_TYPE -> handleSetLobbyType(request)
                Type.SET_GAME_DURATION -> handleSetGameDuration(request)
                Type.JOIN_LOBBY -> handleJoinLobby(request)
                Type.LEAVE_LOBBY -> handleLeaveLobby(request)
                Type.START_GAME -> handleStartGame(request)
                Type.END_GAME -> handleEndGame(request)
                Type.CREATE_SESSION -> handleCreateSession(request)
                Type.REMOVE_SESSION -> handleRemoveSession(request)
                Type.GET_SESSIONS -> handleGetSessions(request)
                Type.CHANGE_SESSIONS_ORDER -> handleChangeSessionsOrder(request)

                else -> Response.getError("Invalid message type")
            }
        } catch (e: Exception) {
            log.error("Error handling game request", e)
            return Response.getError(e)
        }
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

    fun onClientDisconnect(clientHandler: ClientHandler){
        log.debug("onClientDisconnect() with clientId: {}",clientHandler.id)
        val lobbyId = clientIdToLobbyId[clientHandler.id] ?: run {
            // client not in a lobby, nothing to do
            log.debug("onClientDisconnect: Player not in lobby")
            return
        }
        log.debug("onClientDisconnect: Player was in lobby: {}, removing player from lobby",lobbyId)
        val game = clientIdToGame[clientHandler.id]
        if(game != null){
            log.debug("onClientDisconnect: Player was in game, ending game")
            val gameEndRequest = GameRequest.builder(Type.END_GAME)
                .lobbyId(lobbyId)
                .build()
            handleEndGame(gameEndRequest, "Player ${clientHandler.id} disconnected")
        }
        clientIdToLobbyId.remove(clientHandler.id)
        log.debug("onClientDisconnect() successful")
    }

    fun onClientReconnect(client: ClientHandler) {
        log.debug("onClientReconnect() with clientId: {}",client.id)

        // ================== check if the client was in a lobby ================== |

        val lobbyId = clientIdToLobbyId[client.id] ?: run {
            // client not in a lobby, nothing to do
            log.debug("onClientReconnect: Player not in lobby")
            return
        }
        log.debug("onClientReconnect: Player was in lobby: {}, adding player back to lobby",lobbyId)
        val lobby = lobbies[lobbyId] ?: run {
            // should not happen
            log.error("onClientReconnect: lobbyId found for client, but Lobby not found. client: {}",client.id)
            return
        }
        GameRequest.builder(Type.JOIN_LOBBY)
            .lobbyId(lobbyId)
            .gameType(lobby.gameType)
            .build().toJson()
            .also{ client.sendTcp(it) } // notify the client

        // ================== check if the client was in a game ================== |

        val game = clientIdToGame[client.id] ?: run {
            // client not in a game, nothing to do
            log.debug("onClientReconnect: Player not in game")
            return
        }
        log.debug("onClientReconnect: Player was in game, rejoining player to game")
        GameRequest.builder(Type.RECONNECT_TO_GAME)
            .timestamp(game.startTime.toString())
            .build().toJson()
            .also{ client.sendTcp(it) } // notify the client

        log.debug("onClientReconnect() successful")
    }

    // ========================================================================== |
    // ========================= PRIVATE METHODS ================================ |
    // ========================================================================== |

    private fun handleGetOnlinePlayerIds(): String {
        val playerIds = clientController.getAllClientIds()
        return Response.getOk(playerIds)
    }

    private fun handleToggleReady(clientHandler: ClientHandler) {
        log.debug("handleToggleReady() with clientId: {}",clientHandler.id)

        // ========= parameter validation ========= |
        val lobbyId = clientIdToLobbyId[clientHandler.id] ?: run {
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

        val success = lobby.remove(clientId) // true if player was in the lobby
        return if(success){
            clientIdToLobbyId.remove(clientId)
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
        // === check that player is not already in a lobby === |
        if(clientIdToLobbyId.containsKey(clientId)){
            log.debug("handleJoinLobby: Player is already in a lobby")
            return Response.getError("Player is already in a lobby")
        }
        // ======================================== |

        // Try to add the player to the lobby
        val success = lobby.add(clientId)

        return if(success){
            clientIdToLobbyId[clientId] = lobbyId
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

        val lobbyId = lobbyIdGenerator.generate()
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

    private fun handleEndGame(request: GameRequest, errorMessage: String? = null) : String {
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

        game.endGame(errorMessage) // game.endGame() notifies the clients
        clientIdToGame.remove(game.player1.id)
        clientIdToGame.remove(game.player2.id)
        games.remove(lobby.id)
        lobby.state = LobbyState.WAITING
        
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
                GameType.WINE_GLASSES -> {
                    log.debug("handleStartGame: Selected WineGlassesGame")
                    WineGlassesGame(player1Handler, player2Handler)
                }
                GameType.FLOUR_MILL -> {
                    log.debug("handleStartGame: Selected FlourMillGame")
                    FlourMillGame(player1Handler, player2Handler)
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

    private fun handleSetGameDuration(request: GameRequest): String {
        log.debug("handleSetGameDuration() with lobbyId: {}, duration: {}",request.lobbyId, request.duration)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: "
        val missingParams = mutableListOf<String>()
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val duration = request.duration ?: run { missingParams.add("duration") ; null }
        if(lobbyId == null || duration == null){
            log.debug("handleSetGameDuration: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("handleSetGameDuration: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        lobby.gameDuration = duration
        // Notify the clients
        lobby.getPlayers()
            .map {clientController.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                GameRequest.builder(Type.SET_GAME_DURATION)
                    .data(listOf(duration.toString()))
                    .build().toJson()
            )
        }

        log.debug("handleSetGameDuration() successful")
        return Response.getOk()
    }

    private fun handleCreateSession(request: GameRequest): String {
        log.debug("handleCreateSession() with lobbyId: {}, duration: {}, gameType: {}",request.lobbyId,request.data?.get(0),request.gameType)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: "
        val missingParams = mutableListOf<String>()
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val duration = request.duration ?: run { missingParams.add("duration") ; null }
        /*(3)*/ val gameType = request.gameType ?: run { missingParams.add("gameType") ; null }
        if(lobbyId == null || duration == null || gameType == null){
            log.debug("handleCreateSession: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
            log.debug("handleCreateSession: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // ======================================== |

        val sessionId = sessionIdGenerator.generate()
        val session = Session(sessionId, duration, gameType)
        val lobbySessions = lobbyIdToSessions.computeIfAbsent(lobbyId){ ConcurrentLinkedDeque() }
        lobbySessions.add(session)
        return Response.getOk(session.sessionId)
    }

    private fun handleRemoveSession(request: GameRequest): String {
        log.debug("handleRemoveSession() with lobbyId: {}, sessionId: {}",request.lobbyId,request.sessionId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: "
        val missingParams = mutableListOf<String>()
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val sessionId = request.sessionId ?: run { missingParams.add("sessionId") ; null }
        if(lobbyId == null || sessionId == null){
            log.debug("handleRemoveSession: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
            log.debug("handleRemoveSession: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // === check if the lobby has any sessions === |
        val lobbySessions = lobbyIdToSessions[lobbyId] ?: run {
            log.debug("handleRemoveSession: No sessions found for lobby")
            return Response.getError("No sessions found for lobby")
        }
        // ======================================== |

        val success = lobbySessions.removeIf {it.sessionId == sessionId}
        return if(success){
            log.debug("handleRemoveSession() successful")
            Response.getOk()
        } else {
            log.debug("handleRemoveSession() failed: Session not found")
            Response.getError("Session not found")
        }
    }

    private fun handleGetSessions(request: GameRequest): String {
        log.debug("handleGetSessions() with lobbyId: {}",request.lobbyId)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: lobbyId"
        val lobbyId = request.lobbyId ?: run {
            log.debug("handleGetSessions: {}",errorMsg)
            return Response.getError(errorMsg)
        }
        // ======================================== |

        val lobbySessions = lobbyIdToSessions[lobbyId] ?: emptyList()
        return Response.getOk(lobbySessions)
    }

    private fun handleChangeSessionsOrder(request: GameRequest): String {
        log.debug("handleChangeSessionsOrder() with lobbyId: {}, sessionIds: {}",request.lobbyId,request.sessionIds)

        // ========= parameter validation ========= |
        val errorMsg = "Missing the following parameters: "
        val missingParams = mutableListOf<String>()
        /*(1)*/ val lobbyId = request.lobbyId ?: run { missingParams.add("lobbyId") ; null }
        /*(2)*/ val sessionIds = request.sessionIds ?: run { missingParams.add("sessionIds") ; null }
        if(lobbyId == null || sessionIds == null){
            log.debug("handleChangeSessionsOrder: {} {}",errorMsg, missingParams.joinToString())
            return Response.getError("$errorMsg ${missingParams.joinToString()}")
        }
        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
            log.debug("handleChangeSessionsOrder: Lobby not found")
            return Response.getError("Lobby not found")
        }
        // === check if the lobby has any sessions === |
        val lobbySessions = lobbyIdToSessions[lobbyId] ?: run {
            log.debug("handleChangeSessionsOrder: No sessions found for lobby")
            return Response.getError("No sessions found for lobby")
        }
        // ======================================== |

        // validate that the same number of sessions are provided
        if(lobbySessions.size != sessionIds.size){
            log.debug("handleChangeSessionsOrder: Different number of sessions provided")
            return Response.getError("Different number of sessions provided")
        }

        // validate that all the sessions are in the lobby
        if(! sessionIds.all {sessionId -> lobbySessions.any {it.sessionId == sessionId}}){
            log.debug("handleChangeSessionsOrder: Not all sessions are in the lobby")
            return Response.getError("Not all sessions are in the lobby")
        }

        val newOrder = sessionIds.mapNotNull {sessionId ->
            lobbySessions.find {it.sessionId == sessionId}
        }
        lobbySessions.clear()
        lobbySessions.addAll(newOrder)
        return Response.getOk()
    }

    companion object{
        private val log = LoggerFactory.getLogger(GameController::class.java)
    }
}
