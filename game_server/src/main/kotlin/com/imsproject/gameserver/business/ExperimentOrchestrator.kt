package com.imsproject.gameserver.business

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class ExperimentOrchestrator(
    private val lobbies: LobbyController,
    private val sessions: SessionController,
    private val games: GameController,
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

        val job = scope.launch {
            lobby.experimentRunning = true
            val iterator = experimentSessions.iterator()
            while(iterator.hasNext() && isActive) {
                val session = iterator.next()
                lobbies.configureLobby(lobbyId, session)
                while(! lobby.isReady()){
                    delay(1000)
                }
                games.startGame(lobbyId)
                delay(session.duration.toLong()*1000)
                games.endGame(lobbyId)
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
            lobbies.configureLobby(lobbyId, sessions.getSessions(lobbyId).first())
        }
        log.debug("stopExperiment() successful")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExperimentOrchestrator::class.java)
    }
}