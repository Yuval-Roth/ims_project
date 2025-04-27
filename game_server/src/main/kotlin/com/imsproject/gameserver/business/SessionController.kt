package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.SimpleIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

@Component
class SessionController(
    private val lobbies: LobbyController
) {

    private val lobbyIdToSessions = ConcurrentHashMap<String, ConcurrentLinkedDeque<Session>>()
    private val sessionIdGenerator = SimpleIdGenerator(5)

    fun createSession(
        lobbyId: String,
        gameType: GameType,
        duration: Int,
        syncWindowLength: Long,
        syncTolerance: Long
    ): String {
        log.debug("createSession() with lobbyId: {}, gameType: {}, duration: {}, syncWindowLength: {}, syncTolerance: {}",
            lobbyId,gameType,duration,syncWindowLength,syncTolerance)

        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("createSession: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }
        // ======================================== |

        val invalidArgs = mutableListOf<String>()
        val errorMsg = "The following params are invalid: "
        if(duration <= 0) invalidArgs.add("duration")
        if(gameType == GameType.WINE_GLASSES){
            if(syncWindowLength <= 0) invalidArgs.add("syncWindowLength")
        }
        if(syncTolerance <= 0) invalidArgs.add("syncTolerance")
        if(invalidArgs.isNotEmpty()){
            log.debug("createSession: Invalid arguments: {}",invalidArgs.joinToString())
            throw IllegalArgumentException("$errorMsg ${invalidArgs.joinToString()}")
        }

        val sessionId = sessionIdGenerator.generate()
        val session = Session(sessionId, gameType, duration, syncWindowLength, syncTolerance)
        val lobbySessions = lobbyIdToSessions.computeIfAbsent(lobbyId){ ConcurrentLinkedDeque() }
        lobbySessions.add(session)
        if(lobbySessions.size == 1){
            lobbies.configureLobby(lobbyId, session)
        }
        lobby.hasSessions = true
        log.debug("createSession() successful")
        return session.sessionId
    }

    fun removeSession(lobbyId: String, sessionId: String) {
        log.debug("removeSession() with lobbyId: {}, sessionId: {}",lobbyId,sessionId)

        // === check if the lobby exists === |
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("removeSession: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }
        // === check if the lobby has any sessions === |
        val lobbySessions = lobbyIdToSessions[lobbyId] ?: run {
            log.debug("removeSession: No sessions found for lobby")
            throw IllegalArgumentException("No sessions found for lobby")
        }
        // ======================================== |

        val success = lobbySessions.removeIf {it.sessionId == sessionId}
        if(success){
            if(lobbySessions.isEmpty()){
                lobbyIdToSessions.remove(lobbyId)
                lobby.hasSessions = false
            }
            log.debug("removeSession() successful")
        } else {
            log.debug("removeSession() failed: Session not found")
            throw IllegalArgumentException("Session not found")
        }
    }

    fun getSessions(lobbyId: String): Collection<Session> {
        if(! lobbies.contains(lobbyId)){
            log.debug("getSessions: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        return lobbyIdToSessions[lobbyId] ?: emptyList()
    }

    fun changeSessionsOrder(lobbyId: String, sessionIds: List<String>) {
        log.debug("changeSessionsOrder() with lobbyId: {}, sessionIds: {}",lobbyId,sessionIds.joinToString())

        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
            log.debug("changeSessionsOrder: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }
        // === check if the lobby has any sessions === |
        val lobbySessions = lobbyIdToSessions[lobbyId] ?: run {
            log.debug("changeSessionsOrder: No sessions found for lobby")
            throw IllegalArgumentException("No sessions found for lobby")
        }
        // ======================================== |

        // validate that the same number of sessions are provided
        if(lobbySessions.size != sessionIds.size){
            log.debug("changeSessionsOrder: Different number of sessions provided")
            throw IllegalArgumentException("Different number of sessions provided")
        }

        // validate that all the sessions are in the lobby
        if(! sessionIds.all {sessionId -> lobbySessions.any {it.sessionId == sessionId}}){
            log.debug("changeSessionsOrder: Not all sessions are in the lobby")
            throw IllegalArgumentException("Not all sessions are in the lobby")
        }

        val newOrder = sessionIds.mapNotNull {sessionId ->
            lobbySessions.find {it.sessionId == sessionId}
        }
        lobbySessions.clear()
        lobbySessions.addAll(newOrder)
        lobbies.configureLobby(lobbyId, lobbySessions.first())
        log.debug("changeSessionsOrder() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(SessionController::class.java)
    }
}