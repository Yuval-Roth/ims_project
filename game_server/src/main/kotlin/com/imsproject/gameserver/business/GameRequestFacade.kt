package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.utils.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.reflect

@Component
final class GameRequestFacade(
    private val games: GameController,
    private val sessions: SessionController,
    private val lobbies: LobbyController,
    private val clients: ClientController,
    private val experiments: ExperimentOrchestrator
) {

    // =========================================================================== |
    // ========================= Game Request Handling =========================== |
    // =========================================================================== |

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
                Type.START_EXPERIMENT -> handleStartExperiment(request)

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
            Type.TOGGLE_READY -> lobbies.toggleReady(clientHandler)
            else -> throw IllegalArgumentException("Invalid message type: ${request.type}")
        }
    }

    private fun handleGetOnlinePlayerIds() : String {
        return Response.getOk(clients.getAllClientIds())
    }

    private fun handleGetAllLobbies() : String {
        return Response.getOk(lobbies.getLobbiesInfo())
    }

    private fun handleGetLobby(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            lobbies.getLobby(lobbyId)
        }
    }

    private fun handleCreateLobby(request: GameRequest) : String {
        return requireParams(request, "gameType") {
            val gameType = request.gameType!!
            lobbies.createLobby(gameType)
        }
    }

    private fun handleRemoveLobby(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            lobbies.removeLobby(lobbyId)
        }
    }

    private fun handleSetLobbyType(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "gameType") {
            val lobbyId = request.lobbyId!!
            val gameType = request.gameType!!
            lobbies.setLobbyType(lobbyId, gameType)
        }
    }

    private fun handleSetGameDuration(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "duration") {
            val lobbyId = request.lobbyId!!
            val duration = request.duration!!
            lobbies.setGameDuration(lobbyId, duration)
        }
    }

    private fun handleJoinLobby(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "playerId") {
            val lobbyId = request.lobbyId!!
            val clientId = request.playerId!!
            lobbies.joinLobby(lobbyId, clientId)
        }
    }

    private fun handleLeaveLobby(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "playerId") {
            val lobbyId = request.lobbyId!!
            val clientId = request.playerId!!
            lobbies.leaveLobby(lobbyId, clientId)
        }
    }

    private fun handleStartGame(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            games.startGame(lobbyId)
        }
    }

    private fun handleEndGame(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            games.endGame(lobbyId)
        }
    }

    private fun handleCreateSession(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "duration", "gameType") {
            val lobbyId = request.lobbyId!!
            val duration = request.duration!!
            val gameType = request.gameType!!
            sessions.createSession(lobbyId, duration, gameType)
        }
    }

    private fun handleRemoveSession(request: GameRequest) : String {
        return requireParams(request, "lobbyId","sessionId") {
            val lobbyId = request.lobbyId!!
            val sessionId = request.sessionId!!
            sessions.removeSession(lobbyId, sessionId)
        }
    }

    private fun handleGetSessions(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            sessions.getSessions(lobbyId)
        }
    }

    private fun handleChangeSessionsOrder(request: GameRequest) : String {
        return requireParams(request, "lobbyId", "sessionIds") {
            val lobbyId = request.lobbyId!!
            val sessionIds = request.sessionIds!!
            sessions.changeSessionsOrder(lobbyId, sessionIds)
        }
    }

    private fun handleStartExperiment(request: GameRequest) : String {
        return requireParams(request, "lobbyId") {
            val lobbyId = request.lobbyId!!
            experiments.startExperiment(lobbyId)
        }
    }

    /**
     * Returns an error response if any of the required parameters are missing.
     * Otherwise, returns null
     */
    private inline fun <reified T: Any> requireParams(request: GameRequest, vararg params: String, onOk: () -> T) : String {
        val missingParams = mutableListOf<String>()
        for(param in params){
            val field = GameRequest::class.memberProperties.find { it.name == param } ?: throw IllegalArgumentException("Invalid parameter: $param")
            field.get(request) ?: missingParams.add(param)
        }
        if(missingParams.isNotEmpty()){
            val errorMsg = "Received a request of type ${request.type} that is missing the following parameters:"
            val paramsString = missingParams.joinToString()
            log.debug("{} {}",errorMsg, paramsString)
            return Response.getError("$errorMsg $paramsString")
        }

        return if(T::class == Unit::class) {
            onOk()
            Response.getOk()
        } else {
            Response.getOk(onOk())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GameRequestFacade::class.java)
    }
}