package com.imsproject.gameserver.business

import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.dataAccess.SectionEnum
import com.imsproject.gameserver.dataAccess.implementations.SessionPK
import com.imsproject.gameserver.dataAccess.models.ExperimentDTO
import com.imsproject.gameserver.dataAccess.models.SessionDTO
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class ExperimentOrchestrator(
    private val lobbies: LobbyController,
    private val sessions: SessionController,
    private val games: GameController,
    private val daoController: DAOController
) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val ongoingExperiments = mutableMapOf<String, Job>() // lobbyId to experiment

    fun startExperiment(lobbyId: String) {
        log.debug("startExperiment() with lobbyId: {}",lobbyId)

        // check that the lobby exists
        val lobby = lobbies[lobbyId] ?: run{
            log.debug("startExperiment: Lobby with id $lobbyId not found")
            throw IllegalArgumentException("Lobby with id $lobbyId not found")
        }

        val experimentSessions = sessions.getSessions(lobbyId)
        if(experimentSessions.isEmpty()){
            log.debug("startExperiment: No sessions found for lobby $lobbyId")
            throw IllegalArgumentException("No sessions found for lobby $lobbyId")
        }

        val player1Id = lobby.player1Id ?: run{
            log.debug("startExperiment: Player 1 not found for lobby $lobbyId")
            throw IllegalArgumentException("Player 1 not found for lobby $lobbyId")
        }
        val player2Id = lobby.player2Id ?: run{
            log.debug("startExperiment: Player 2 not found for lobby $lobbyId")
            throw IllegalArgumentException("Player 2 not found for lobby $lobbyId")
        }

        val pid1 = Integer.parseInt(player1Id)
        val pid2 = Integer.parseInt(player2Id)

        val experimentDTO = ExperimentDTO(null,pid1,pid2)
        val experimentId = daoController.handleInsert(SectionEnum.EXPERIMENT, experimentDTO)

        val job = scope.launch {
            lobby.experimentRunning = true
            val iterator = experimentSessions.iterator()
            var sessionIndex = 1
            while(iterator.hasNext() && isActive) {
                val session = iterator.next()
                lobbies.configureLobby(lobbyId, session)

                val sessionDTO = SessionDTO(
                    null,
                    experimentId,
                    session.duration,
                    session.gameType.name,
                    sessionIndex++,
                    session.syncTolerance.toInt(),
                    session.syncWindowLength.toInt(),
                    SessionState.NOT_STARTED.name
                )
                val sessionId = daoController.handleInsert(SectionEnum.SESSION, sessionDTO)
                session.dbId = sessionId
                while(! lobby.isReady()){
                    delay(1000)
                }
                games.startGame(lobbyId)
                session.state = SessionState.IN_PROGRESS
                delay(session.duration.toLong()*1000)
                games.endGame(lobbyId)
                val updatedSessionDTO = SessionDTO(sessionId = sessionId, state = SessionState.COMPLETED.name)
                daoController.handleUpdate(SectionEnum.SESSION, updatedSessionDTO)
                sessions.removeSession(lobbyId, session.sessionId)
            }
            lobby.experimentRunning = false
        }
        ongoingExperiments[lobbyId] = job
        log.debug("startExperiment() successful")
    }

    fun stopExperiment(lobbyId: String) {
        log.debug("stopExperiment() with lobbyId: {}",lobbyId)

        val lobby = lobbies[lobbyId] ?: run{
            log.debug("stopExperiment: Lobby with id $lobbyId not found")
            throw IllegalArgumentException("Lobby with id $lobbyId not found")
        }

        val job = ongoingExperiments[lobbyId] ?: run{
            log.debug("stopExperiment: No experiment found for lobby $lobbyId")
            throw IllegalArgumentException("No experiment found for lobby $lobbyId")
        }

        job.cancel()
        runBlocking {
            job.join()
        }
        ongoingExperiments.remove(lobbyId)
        lobby.experimentRunning = false
        games.endGame(lobbyId)
        if(lobby.hasSessions){
            val session = sessions.getSessions(lobbyId).first()
            lobbies.configureLobby(lobbyId, session)
            if(session.state == SessionState.IN_PROGRESS){
                val dbId = session.dbId ?: run {
                    // should not happen
                    log.error("stopExperiment: Session dbId not found for in-progress session in lobby $lobbyId")
                    throw IllegalStateException("Session dbId not found for in-progress session in lobby $lobbyId")
                }
                val updatedSessionDTO = SessionDTO(sessionId = dbId, state = SessionState.CANCELLED.name)
                daoController.handleUpdate(SectionEnum.SESSION, updatedSessionDTO)
                sessions.removeSession(lobbyId, session.sessionId)
            }
        }
        log.debug("stopExperiment() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExperimentOrchestrator::class.java)
    }
}