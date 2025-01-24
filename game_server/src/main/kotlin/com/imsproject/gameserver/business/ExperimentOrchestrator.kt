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
            while(iterator.hasNext()) {
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
        }
        ongoingExperiments[lobbyId] = job
    }

    fun stopExperiment(lobbyId: String){

    }

    companion object {
        private val log = LoggerFactory.getLogger(ExperimentOrchestrator::class.java)
    }
}