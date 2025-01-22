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
        duration: Int = -1,
        syncWindowLength: Long = -1,
        syncTolerance: Long = -1
    ): String {
        log.debug("createSession() with lobbyId: {}, duration: {}, gameType: {}",lobbyId,duration,gameType)

        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
            log.debug("createSession: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }
        // ======================================== |

        val sessionId = sessionIdGenerator.generate()
        val session = Session(sessionId, duration, gameType, syncWindowLength, syncTolerance)
        val lobbySessions = lobbyIdToSessions.computeIfAbsent(lobbyId){ ConcurrentLinkedDeque() }
        lobbySessions.add(session)
        return session.sessionId
    }

    fun removeSession(lobbyId: String, sessionId: String) {
        log.debug("removeSession() with lobbyId: {}, sessionId: {}",lobbyId,sessionId)

        // === check if the lobby exists === |
        if(! lobbies.contains(lobbyId)){
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
            }
            log.debug("removeSession() successful")
        } else {
            log.debug("removeSession() failed: Session not found")
            throw IllegalArgumentException("Session not found")
        }
    }

    fun getSessions(lobbyId: String): Collection<Session> {
        log.debug("getSessions() with lobbyId: {}",lobbyId)

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
    }

    companion object {
        private val log = LoggerFactory.getLogger(SessionController::class.java)
    }
}