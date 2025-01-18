package com.imsproject.gameserver.business

import com.imsproject.common.utils.SimpleIdGenerator
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class ExperimentOrchestrator(
    private val lobbies: LobbyController,
    private val sessions: SessionController,
    private val games: GameController
) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val ongoingExperiments = mutableMapOf<String, Job>() // lobbyId to experiment

    fun startExperiment(lobbyId: String) {

        val notFound = mutableListOf<String>()
        val errorMsg = "The following were not found:"
        /*(1)*/ val lobby = lobbies[lobbyId] ?: run{ notFound.add("Lobby") ; null }
        /*(2)*/ val sessions = sessions[lobbyId] ?: run{ notFound.add("Sessions") ; null }
        if(lobby == null || sessions == null){
            val joinedString = notFound.joinToString()
            log.error("startExperiment: {} {}",errorMsg, joinedString)
            throw IllegalArgumentException("$errorMsg $joinedString")
        }

        val job = scope.launch {
            val iterator = sessions.iterator()
            while(iterator.hasNext()) {
                val session = iterator.next()
                lobbies.setLobbyType(lobbyId, session.gameType)
                lobbies.setGameDuration(lobbyId, session.duration)
                while(! lobby.isReady()){
                    delay(1000)
                }
                games.startGame(lobbyId)
                delay(session.duration.toLong())
                games.endGame(lobbyId)
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