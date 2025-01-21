package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.business.lobbies.Lobby
import com.imsproject.gameserver.business.lobbies.LobbyInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class LobbyController(
    private val clients: ClientController
) {

    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val clientIdToLobbyId = ConcurrentHashMap<String, String>()
    private val lobbyIdGenerator = SimpleIdGenerator(4)
    
    operator fun get(lobbyId: String): Lobby? {
        return lobbies[lobbyId]
    }
    
    operator fun set(lobbyId: String, lobby: Lobby) {
        lobbies[lobbyId] = lobby
    }
    
    operator fun contains(lobbyId: String): Boolean {
        return lobbies.containsKey(lobbyId)
    }
    
    fun remove(lobbyId: String) {
        lobbies.remove(lobbyId)
    }
    
    fun getByClientId(clientId: String): Lobby? {
        return clientIdToLobbyId[clientId]?.let { lobbies[it] }
    }
    
    fun removeByClientId(clientId: String) {
        clientIdToLobbyId.remove(clientId)
    }

    fun createLobby(gameType: GameType) : String {
        log.debug("createLobby() with gameType: {}", gameType)

        val lobbyId = lobbyIdGenerator.generate()
        val lobby = Lobby(lobbyId,gameType)
        lobbies[lobbyId] = lobby
        log.debug("createLobby() successful")
        return lobbyId
    }

    fun removeLobby(lobbyId: String) {
        log.debug("removeLobby() with lobbyId: {}", lobbyId)

        // check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("removeLobby: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        lobbies.remove(lobbyId)
        // Notify the clients
        lobby.getPlayers()
            .map {clients.getByClientId(it)}
            .forEach {
                it?.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            }

        log.debug("removeLobby() successful")
    }

    fun toggleReady(clientHandler: ClientHandler) {
        log.debug("toggleReady() with clientId: {}",clientHandler.id)

        // ========= parameter validation ========= |
        val lobbyId = clientIdToLobbyId[clientHandler.id] ?: run {
            log.debug("toggleReady: Player not in lobby")
            throw IllegalArgumentException("Player not in lobby")
        }
        val lobby = lobbies[lobbyId] ?: run {
            // should not happen
            log.error("toggleReady: lobbyId found for client by Lobby not found. client: {}",clientHandler.id)
            throw IllegalArgumentException("Lobby not found")
        }
        // ======================================== |

        val success = lobby.toggleReady(clientHandler.id)
        if (success) {
            log.debug("toggleReady() successful")
        } else {
            // should not happen
            log.error("toggleReady() failed: Lobby found for player but toggle ready failed")
            throw IllegalArgumentException("Toggle ready failed")
        }
    }

    fun getLobby(lobbyId: String) : LobbyInfo {
        // check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("getLobby: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        return lobby.getInfo()
    }

    fun leaveLobby(lobbyId: String,clientId: String) {
        log.debug("leaveLobby() with lobbyId: {}, playerId: {}",lobbyId,clientId)

        // check if the lobby and player exist
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run { notFound.add("Lobby") ; null }
        /*(2)*/ val clientHandler = clients.getByClientId(clientId) ?: run { notFound.add("Player") ; null }
        if(lobby == null || clientHandler == null){
            log.debug("leaveLobby: {} {}",errorMsg2, notFound.joinToString())
            throw IllegalArgumentException("$errorMsg2 ${notFound.joinToString()}")
        }

        val success = lobby.remove(clientId) // true if player was in the lobby
        if(success){
            clientIdToLobbyId.remove(clientId)
            // notify the client
            clientHandler.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            log.debug("leaveLobby() successful")
        } else {
            log.debug("leaveLobby() failed: Player not in lobby")
            throw IllegalArgumentException("Player not in lobby")
        }
    }

    fun joinLobby(lobbyId: String, clientId: String) {
        log.debug("joinLobby() with lobbyId: {}, playerId: {}",lobbyId,clientId)

        // check if the lobby and player exist
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run { notFound.add("Lobby") ; null }
        /*(2)*/ val clientHandler = clients.getByClientId(clientId) ?: run { notFound.add("Player") ; null }
        if(lobby == null || clientHandler == null){
            log.debug("joinLobby: {} {}",errorMsg2, notFound.joinToString())
            throw IllegalArgumentException("$errorMsg2 ${notFound.joinToString()}")
        }

        // check if the player is already in a lobby
        if(clientIdToLobbyId.containsKey(clientId)){
            log.debug("joinLobby: Player is already in a lobby")
            throw IllegalArgumentException("Player is already in a lobby")
        }

        // Try to add the player to the lobby
        val success = lobby.add(clientId)

        if(success){
            clientIdToLobbyId[clientId] = lobbyId
            // notify the client
            clientHandler.sendTcp(
                GameRequest.builder(Type.JOIN_LOBBY)
                    .lobbyId(lobbyId)
                    .gameType(lobby.gameType)
                    .build().toJson())
            log.debug("joinLobby() successful")
        } else {
            log.debug("joinLobby() failed: Lobby is full")
            throw IllegalArgumentException("Lobby is full")
        }
    }

    fun getLobbiesInfo() : List<LobbyInfo> {
        log.debug("getLobbiesInfo()")

        return lobbies.values.map { it.getInfo() }
    }

    fun setLobbyType(lobbyId: String, gameType: GameType) {
        log.debug("setLobbyType() with lobbyId: {}, gameType: {}",lobbyId,gameType)

        // Check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("setLobbyType: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        lobby.gameType = gameType
        // Notify the clients
        lobby.getPlayers()
            .map {clients.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                    GameRequest.builder(Type.SET_LOBBY_TYPE)
                        .gameType(gameType)
                        .build().toJson()
                )
            }

        log.debug("setLobbyType() successful")
    }

    fun setGameDuration(lobbyId: String, duration: Int) {
        log.debug("setGameDuration() with lobbyId: {}, duration: {}",lobbyId, duration)

        // Check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("setGameDuration: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        lobby.gameDuration = duration
        // Notify the clients
        lobby.getPlayers()
            .map {clients.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                    GameRequest.builder(Type.SET_GAME_DURATION)
                        .data(listOf(duration.toString()))
                        .build().toJson()
                )
            }

        log.debug("setGameDuration() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(LobbyController::class.java)
    }
}