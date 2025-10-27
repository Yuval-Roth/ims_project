package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.lobbies.Lobby
import com.imsproject.gameserver.business.lobbies.LobbyInfo
import com.imsproject.gameserver.business.lobbies.LobbyState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class LobbyService(
    private val clients: ClientService
) {

    init{
        clients.onClientDisconnect = { onClientDisconnect(it) }
    }

    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val clientIdToLobbyId = ConcurrentHashMap<String, String>()
    private val lobbyIdGenerator = SimpleIdGenerator(4)
    
    operator fun get(lobbyId: String): Lobby? {
        return lobbies[lobbyId]
    }
    
    operator fun contains(lobbyId: String): Boolean {
        return lobbies.containsKey(lobbyId)
    }
    
    fun getByClientId(clientId: String): Lobby? {
        return clientIdToLobbyId[clientId]?.let { lobbies[it] }
    }

    fun onClientDisconnect(clientHandler: ClientHandler){
        log.debug("onClientDisconnect() with clientId: {}",clientHandler.id)
        val lobbyId = clientIdToLobbyId[clientHandler.id]
        if(lobbyId != null){
            log.debug("onClientDisconnect: Player was in lobby: {}, removing player from lobby",lobbyId)
            leaveLobby(lobbyId, clientHandler.id,false)
        } else  {
            log.debug("onClientDisconnect: Player not in lobby")
        }
        log.debug("onClientDisconnect() successful")
    }

    fun onClientConnect(clientHandler: ClientHandler){
        log.debug("onClientConnect() with clientId: {}",clientHandler.id)
        val lobbyId = clientIdToLobbyId[clientHandler.id] ?: run{
            log.debug("onClientConnect: Player not in lobby")
            return
        }
        val lobby = lobbies[lobbyId] ?: run {
            // should not happen
            log.error("onClientConnect: Lobby found for client but not in lobbies map client: {}",clientHandler.id)
            clientIdToLobbyId.remove(clientHandler.id)
            return
        }
        log.debug("onClientConnect: Player was in lobby: {}, sending join lobby message",lobbyId)
        clientHandler.sendTcp(
            GameRequest.builder(Type.JOIN_LOBBY)
                .lobbyId(lobbyId)
                .build().toJson()
        )
        if(lobby.hasSessions){
            log.debug("onClientConnect: Sending configure lobby message")
            clientHandler.sendTcp(
                GameRequest.builder(Type.CONFIGURE_LOBBY)
                    .gameType(lobby.gameType)
                    .duration(lobby.gameDuration)
                    .syncWindowLength(lobby.syncWindowLength)
                    .syncTolerance(lobby.syncTolerance)
                    .isWarmup(lobby.isWarmup)
                    .build().toJson()
            )
        }

        log.debug("onClientConnect() successful")
    }

    fun createLobby() : String {
        log.debug("createLobby()")

        val lobbyId = lobbyIdGenerator.generate()
        val lobby = Lobby(lobbyId)
        lobbies[lobbyId] = lobby
        log.debug("createLobby() successful with lobbyId: {}",lobbyId)
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
        val players = lobby.getPlayers()
        players.forEach { clientIdToLobbyId.remove(it) }
        players.map {clients.getByClientId(it)}
            .forEach { it?.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson()) }

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
            log.error("toggleReady: lobbyId found for client but Lobby not found. client: {}",clientHandler.id)
            clientIdToLobbyId.remove(clientHandler.id)
            clientHandler.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            return
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
            // we're not logging cuz it's spamming the log files,
            // and it happens every time a lobby is removed
//            log.debug("getLobby: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        return lobby.getInfo()
    }

    fun leaveLobby(lobbyId: String,clientId: String,notifyClient: Boolean = true) {
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
            if(notifyClient){
                clientHandler.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            }
            if(lobby.isEmpty()){
                lobbies.remove(lobbyId)
            }
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
            val currentLobbyId = clientIdToLobbyId[clientId]
            if(currentLobbyId != null){
                log.debug("joinLobby: Player is already in a lobby")
                throw IllegalArgumentException("Player is already in a lobby")
            } else {
                // should not happen
                log.error("joinLobby: Player $clientId is in a lobby but lobbyId not found, resetting player lobby mapping")
                clientIdToLobbyId.remove(clientId)
            }
        }

        // Try to add the player to the lobby
        val success = lobby.add(clientId)

        if(success){
            clientIdToLobbyId[clientId] = lobbyId
            // notify the client
            clientHandler.sendTcp(
                GameRequest.builder(Type.JOIN_LOBBY)
                    .lobbyId(lobbyId)
                    .build().toJson())
            if(lobby.hasSessions){
                sendLobbyConfiguration(clientHandler)
            }
            log.debug("joinLobby() successful")
        } else {
            log.debug("joinLobby() failed: Lobby is full")
            throw IllegalArgumentException("Lobby is full")
        }
    }

    fun getLobbiesInfo() : List<LobbyInfo> {
        return lobbies.values.map { it.getInfo() }
    }

    fun configureLobby(lobbyId: String, sessionDetails: Session){
        log.debug("configureLobby() with lobbyId: {}, sessionDetails: {}",lobbyId,sessionDetails)

        // Check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("configureLobby: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        if(lobby.state != LobbyState.WAITING){
            log.debug("configureLobby: Lobby is not in waiting state")
            throw IllegalStateException("Lobby is not in waiting state")
        }

        lobby.configure(sessionDetails)

        // Notify the clients
        lobby.getPlayers()
            .map {clients.getByClientId(it)}
            .forEach {
                it?.sendTcp(
                    GameRequest.builder(Type.CONFIGURE_LOBBY)
                        .gameType(sessionDetails.gameType)
                        .duration(sessionDetails.duration)
                        .syncWindowLength(sessionDetails.syncWindowLength)
                        .syncTolerance(sessionDetails.syncTolerance)
                        .isWarmup(sessionDetails.isWarmup)
                        .build().toJson()
                )
            }

        log.debug("configureLobby() successful")
    }

    fun isClientInALobby(clientId: String): Boolean {
        return clientIdToLobbyId.containsKey(clientId)
    }

    fun sendLobbyConfiguration(clientHandler: ClientHandler) {
        log.debug("sendLobbyConfiguration() with clientId: {}",clientHandler.id)

        // ========= parameter validation ========= |
        val lobbyId = clientIdToLobbyId[clientHandler.id] ?: run {
            log.debug("sendLobbyConfiguration: Player not in lobby")
            throw IllegalArgumentException("Player not in lobby")
        }
        val lobby = lobbies[lobbyId] ?: run {
            // should not happen
            log.error("sendLobbyConfiguration: lobbyId found for client but Lobby not found. client: {}",clientHandler.id)
            clientIdToLobbyId.remove(clientHandler.id)
            clientHandler.sendTcp(GameRequest.builder(Type.LEAVE_LOBBY).build().toJson())
            return
        }
        // ======================================== |

        clientHandler.sendTcp(
            GameRequest.builder(Type.CONFIGURE_LOBBY)
                .gameType(lobby.gameType)
                .duration(lobby.gameDuration)
                .syncWindowLength(lobby.syncWindowLength)
                .syncTolerance(lobby.syncTolerance)
                .isWarmup(lobby.isWarmup)
                .build().toJson()
        )
        log.debug("sendLobbyConfiguration() successful")
    }

    fun startExperiment(lobbyId: String) {
        log.debug("startExperiment() with lobbyId: {}",lobbyId)

        // check that the lobby exists
        val lobby = lobbies[lobbyId] ?: run{
            log.debug("startExperiment: Lobby with id $lobbyId not found")
            throw IllegalArgumentException("Lobby with id $lobbyId not found")
        }
        lobby.player1Id?.let { clients.getByClientId(it)?.sendTcp(
            GameRequest.builder(Type.START_EXPERIMENT).data(listOf("blue")).build().toJson()
        )}
        lobby.player2Id?.let { clients.getByClientId(it)?.sendTcp(
            GameRequest.builder(Type.START_EXPERIMENT).data(listOf("green")).build().toJson()
        )}

        log.debug("startExperiment() successful")
    }

    fun signalBothClientsReady(lobbyId: String) {
        log.debug("signalBothClientsReady() with lobbyId: {}",lobbyId)

        // check that the lobby exists
        val lobby = lobbies[lobbyId] ?: run{
            log.debug("signalBothClientsReady: Lobby with id $lobbyId not found")
            throw IllegalArgumentException("Lobby with id $lobbyId not found")
        }

        lobby.getPlayers()
            .map {clients.getByClientId(it)}
            .forEach {
                it?.sendTcp(GameRequest.builder(Type.BOTH_CLIENTS_READY).build().toJson())
            }

        log.debug("signalBothClientsReady() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(LobbyService::class.java)
    }
}