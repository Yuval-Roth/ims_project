package com.imsproject.gameserver.business

import com.imsproject.gameserver.business.lobbies.LobbyState
import com.imsproject.gameserver.dataAccess.DAOController import com.imsproject.gameserver.dataAccess.models.ExperimentDTO
import com.imsproject.gameserver.dataAccess.models.SessionDTO
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import kotlin.math.exp

@Service
class ExperimentOrchestrator(
    private val lobbyService: LobbyService,
    private val sessionService: SessionService,
    private val gameService: GameService,
    private val daoController: DAOController
) {

    init{
        gameService.onMalformedGameTermination = { lobbyId ->
            log.debug("onMalformedGameTermination() with lobbyId: {}",lobbyId)
            stopExperiment(lobbyId)
        }
    }

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val ongoingExperiments = mutableMapOf<String, Job>() // lobbyId to experiment

    fun startExperiment(lobbyId: String) {
        log.debug("startExperiment() with lobbyId: {}",lobbyId)

        // check that the lobby exists
        val lobby = lobbyService[lobbyId] ?: run{
            log.debug("startExperiment: Lobby with id $lobbyId not found")
            throw IllegalArgumentException("Lobby with id $lobbyId not found")
        }

        val experimentSessions = sessionService.getSessions(lobbyId)
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

        val experimentDTO = ExperimentDTO(null,pid1,pid2,null)
        val expId = daoController.handleInsert(experimentDTO)
        experimentSessions.forEachIndexed { index, session ->
            if(session.dbId == null){
                val dto = SessionDTO(
                    null,
                    expId,
                    session.duration,
                    session.gameType.name,
                    index+1,
                    session.syncTolerance.toInt(),
                    session.syncWindowLength.toInt(),
                    SessionState.NOT_STARTED.name
                )
                val sessionId = daoController.handleInsert(dto)
                session.dbId = sessionId
            }
        }

        val job = scope.launch {
            lobby.experimentRunning = true
            val iterator = experimentSessions.iterator()
            while(iterator.hasNext() && isActive) {
                val session = iterator.next()
                lobbyService.configureLobby(lobbyId, session)
                val sessionId = session.dbId ?: run {
                    // should not happen
                    log.error("startExperiment: Session dbId not found for session in lobby $lobbyId")
                    throw IllegalStateException("Session dbId not found for session in lobby $lobbyId")
                }
                while(! lobby.isReady()){
                    delay(1000)
                }
                gameService.startGame(lobbyId,sessionId)
                session.state = SessionState.IN_PROGRESS
                delay(session.duration.toLong()*1000)
                gameService.endGame(lobbyId, if(! iterator.hasNext()) expId else null)
                val updatedSessionDTO = SessionDTO(sessionId = sessionId, state = SessionState.COMPLETED.name)
                daoController.handleUpdate(updatedSessionDTO)
                sessionService.removeSession(lobbyId, session.sessionId)
            }
            lobbyService.removeLobby(lobby.id)
            lobby.experimentRunning = false
        }
        ongoingExperiments[lobbyId] = job
        log.debug("startExperiment() successful")
    }

    fun stopExperiment(lobbyId: String) {
        log.debug("stopExperiment() with lobbyId: {}",lobbyId)

        val lobby = lobbyService[lobbyId] ?: run{
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
        if(lobby.state == LobbyState.PLAYING){
            gameService.endGame(lobbyId)
        }
        if(lobby.hasSessions){
            val currentSession = sessionService.getSessions(lobbyId).first()
            if(currentSession.state == SessionState.IN_PROGRESS){
                val dbId = currentSession.dbId ?: run {
                    // should not happen
                    log.error("stopExperiment: Session dbId not found for in-progress session in lobby $lobbyId")
                    throw IllegalStateException("Session dbId not found for in-progress session in lobby $lobbyId")
                }
                val updatedSessionDTO = SessionDTO(sessionId = dbId, state = SessionState.CANCELLED.name)
                daoController.handleUpdate(updatedSessionDTO)
                sessionService.removeSession(lobbyId, currentSession.sessionId)
            }
            if(lobby.hasSessions){
                val nextSession = sessionService.getSessions(lobbyId).first()
                lobbyService.configureLobby(lobbyId,nextSession)
            }
        }
        log.debug("stopExperiment() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExperimentOrchestrator::class.java)
    }
}