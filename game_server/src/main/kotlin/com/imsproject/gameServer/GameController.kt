package com.imsproject.gameServer

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.common.gameServer.GameRequest.Type
import com.imsproject.common.gameServer.GameType
import com.imsproject.common.gameServer.LobbyState
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.SimpleIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class GameController(private val clientController: ClientController) {

    private val lobbies = ConcurrentHashMap<String,Lobby>()
    private val games = ConcurrentHashMap<String, Game>()
    private val clientIdToGame = ConcurrentHashMap<String, Game>()
    private val idGenerator = SimpleIdGenerator(2)


    /**
     * @throws IllegalArgumentException if anything is wrong with the message
     */
    fun handleGameRequest(clientHandler: ClientHandler, request: GameRequest) {
        when (request.type) {
            Type.TOGGLE_READY -> handleToggleReady(clientHandler, request)
            else -> throw IllegalArgumentException("Invalid message type: ${request.type}")
        }
    }

    private fun handleToggleReady(clientHandler: ClientHandler, request: GameRequest) {
        val lobby = lobbies[request.lobbyId] ?: throw IllegalArgumentException("Lobby not found")
        val success = lobby.toggleReady(clientHandler.id)
        clientHandler.sendTcp(
            GameRequest.builder(Type.TOGGLE_READY)
                .success(success)
                .build().toJson()
        )
    }

    fun handleGameAction(clientHandler: ClientHandler, action: GameAction) {
        TODO("Not yet implemented")
    }

    fun getAllLobbies() : String {
        val lobbiesInfo = lobbies.values.map { it.getInfo().toJson() }
        return Response.getOk(lobbiesInfo)
    }

    fun getLobby(lobbyId: String) : String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")
        return Response.getOk(lobby.getInfo().toJson())
    }

    fun leaveLobby(lobbyId : String, clientId : String): String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")
        if (clientController.containsByClientId(clientId).not()) {
            return Response.getError("Player not found")
        }

        val success = lobby.remove(clientId)
        return if(success){
            Response.getOk()
        } else {
            Response.getError("Player not in lobby")
        }
    }

    fun joinLobby(lobbyId: String, clientId: String) : String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")
        if (clientController.containsByClientId(clientId).not()) {
            return Response.getError("Player not found")
        }

        // Try to add the player to the lobby
        val success = lobby.add(clientId)
        return if(success){
            Response.getOk()
        } else {
            Response.getError("Lobby is full")
        }
    }

    fun createLobby(gameType: String) : String {
        val _gameType: GameType
        try{
            _gameType = GameType.valueOf(gameType)
        } catch (e: IllegalArgumentException){
            return Response.getError("Invalid game type")
        }

        val lobbyId = idGenerator.generate()
        val lobby = Lobby(lobbyId,_gameType)
        lobbies[lobbyId] = lobby
        return Response.getOk(lobbyId)
    }

    fun endGame(lobbyId: String) : String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")
        val game = games[lobbyId] ?: return Response.getError("Game not found")
        game.endGame()
        games.remove(lobby.id)
        lobby.state = LobbyState.WAITING
        lobby.getPlayers()
            .map {clientController.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                GameRequest.builder(Type.END_GAME)
                    .build().toJson()
            )
        }
        return Response.getOk()
    }

    fun startGame(lobbyId: String) : String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")

        // Check if the lobby is ready
        if(lobby.isReady().not()) {
            return Response.getError("Lobby is not ready")
        }

        val bad = Response.getError("Failed to start game")

        // Start the game
        val player1Id = lobby.player1Id!!
        val player2Id = lobby.player2Id!!
        val player1Handler = clientController.getByClientId(player1Id) ?: run{
            log.error("Player 1 handler is null in lobby: $lobbyId")
            return bad
        }
        val player2Handler = clientController.getByClientId(player2Id) ?: run{
            log.error("Player 2 handler is null in lobby: $lobbyId")
            return bad
        }
        val game = when(lobby.gameType){
                GameType.WATER_RIPPLES -> TODO("Not yet implemented")
                GameType.POC -> PocGame(player1Handler, player2Handler)
            }
        lobby.state = LobbyState.PLAYING
        games[lobby.id] = game
        clientIdToGame[player1Id] = game
        clientIdToGame[player2Id] = game
        game.startGame()

        // Notify the players
        val msg = GameRequest.builder(Type.START_GAME).build().toJson()
        player1Handler.sendTcp(msg)
        player2Handler.sendTcp(msg)

        return Response.getOk()
    }

    fun setLobbyType(lobbyId: String, gameType: String): String {
        val lobby = lobbies[lobbyId] ?: return Response.getError("Lobby not found")
        val _gameType: GameType
        try{
            _gameType = GameType.valueOf(gameType)
        } catch (e: IllegalArgumentException){
            return Response.getError("Invalid game type")
        }
        lobby.gameType = _gameType
        return Response.getOk()
    }

    companion object{
        private val log = LoggerFactory.getLogger(GameController::class.java)
    }
}
