package com.imsproject.game_server

import com.imsproject.utils.SimpleIdGenerator
import com.imsproject.utils.gameServer.GameAction
import com.imsproject.utils.gameServer.GameRequest
import com.imsproject.utils.gameServer.GameRequest.Type
import com.imsproject.utils.gameServer.GameType
import com.imsproject.utils.gameServer.LobbyState
import org.springframework.stereotype.Component

@Component
class GameController {

    private val lobbies = mutableMapOf<String,Lobby>()
    private val games = mutableMapOf<String, Game>()
    private val idGenerator = SimpleIdGenerator(2)

    /**
     * @throws IllegalArgumentException if anything is wrong with the message
     */
    fun handleGameRequest(clientHandler: ClientHandler, request: GameRequest) {
        when (request.type) {
            Type.GET_LOBBIES -> handleGetLobbies(clientHandler, request)
            Type.CREATE_LOBBY -> handleCreateLobby(clientHandler, request)
            Type.JOIN_LOBBY -> handleJoinLobby(clientHandler, request)
            Type.LEAVE_LOBBY -> handleLeaveLobby(clientHandler, request)
            Type.START_GAME -> handleStartGame(clientHandler, request)
            Type.END_GAME -> handleEndGame(clientHandler, request)
            Type.PAUSE_GAME -> handlePauseGame(clientHandler, request)
            Type.RESUME_GAME -> handleResumeGame(clientHandler, request)
            else -> throw IllegalArgumentException("Invalid message type: ${request.type}")
        }
    }

    fun handleGameAction(clientHandler: ClientHandler, action: GameAction) {
        TODO("Not yet implemented")
    }

    private fun handleGetLobbies(clientHandler: ClientHandler, message: GameRequest) {
        val lobbiesInfo = lobbies.values.map { it.getInfo().toJson() }
        clientHandler.sendTcp(
            GameRequest.builder(Type.GET_LOBBIES)
                .success(true)
                .data(lobbiesInfo)
                .build().toJson()
        )
    }

    private fun handleLeaveLobby(clientHandler: ClientHandler, message: GameRequest) {
        val lobby = getLobby(message.lobbyId)

        lobby.remove(clientHandler) // this throws an exception if the player is not in the lobby

        clientHandler.sendTcp(
            GameRequest.builder(Type.LEAVE_LOBBY)
                .success(true)
                .build().toJson()
        )
    }

    private fun handleJoinLobby(clientHandler: ClientHandler, message: GameRequest) {
        val lobby = getLobby(message.lobbyId)

        // Try to add the player to the lobby
        val success = lobby.add(clientHandler)
        val messageBuilder = GameRequest.builder(Type.JOIN_LOBBY).success(success)

        // if the player was added, send the lobby info to the player
        if(success){
            messageBuilder.data(listOf(lobby.getInfo().toJson()))
        }

        clientHandler.sendTcp(messageBuilder.build().toJson())
    }

    private fun handleCreateLobby(clientHandler: ClientHandler, message: GameRequest) {
        val type = message.gameType ?: throw IllegalArgumentException("Game type is required")
        val lobbyId = idGenerator.generate()
        val lobby = Lobby(lobbyId,type)
        lobby.add(clientHandler)
        lobbies[lobbyId] = lobby
        clientHandler.sendTcp(
            GameRequest.builder(Type.CREATE_LOBBY)
                .lobbyId(lobbyId)
                .success(true)
                .build().toJson()
        )
    }

    private fun handleEndGame(clientHandler: ClientHandler, message: GameRequest) {
        val lobby = getLobby(message.lobbyId)
        val game = games[lobby.id] ?: throw IllegalArgumentException("Game not found")
        game.endGame()
        games.remove(lobby.id)
        lobby.state = LobbyState.WAITING
        lobby.getPlayers().forEach {
            it.sendTcp(
                GameRequest.builder(Type.END_GAME)
                    .success(true)
                    .build().toJson()
            )
        }
    }

    private fun handleStartGame(host: ClientHandler, message: GameRequest) {
        val lobby = getLobby(message.lobbyId)

        // Check if the host is the one starting the game
        if(lobby.host != host){
            throw IllegalArgumentException("Only the host can start the game")
        }

        // Check if the lobby is ready
        if(lobby.isReady().not()) {
            throw IllegalArgumentException("Lobby is not ready")
        }

        // Start the game
        val game = when(lobby.gameType){
                GameType.WATER_RIPPLES -> TODO("Not yet implemented")
                GameType.POC -> PocGame(lobby.host!!, lobby.guest!!)
            }
        lobby.state = LobbyState.PLAYING
        games[lobby.id] = game
        game.startGame()

        // Notify the players
        lobby.getPlayers().forEach {
            it.sendTcp(
                GameRequest.builder(Type.START_GAME)
                    .success(true)
                    .build().toJson()
            )
        }
    }

    private fun handleResumeGame(clientHandler: ClientHandler, message: GameRequest) {
        TODO("Not yet implemented")
    }

    private fun handlePauseGame(clientHandler: ClientHandler, message: GameRequest) {
        TODO("Not yet implemented")
    }

    private fun getLobby(message: String?): Lobby {
        val lobbyId = message ?: throw IllegalArgumentException("Lobby id is required")
        val lobby = lobbies[lobbyId] ?: throw IllegalArgumentException("Lobby not found")
        return lobby
    }
}
