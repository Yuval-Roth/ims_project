package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.TimeServerService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class PacmanGame(
    lobbyId: String,
    player1: ClientHandler,
    player2: ClientHandler
) : Game(lobbyId, player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.USER_INPUT -> {
                sendGameAction(action)
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

    override fun startGame(sessionId: Int) {
        val timeHandler = TimeServerService.instance
        val timeServerCurr = timeHandler.timeServerCurrentTimeMillis().toString()
        localStartTime =  System.currentTimeMillis() + timeHandler.timeServerDelta
        val toSend = GameRequest.builder(GameRequest.Type.START_GAME)
            .sessionId(sessionId.toString())
            .timestamp(timeServerCurr)
        player1.sendTcp(toSend.data(listOf("left")).build().toJson())
        player2.sendTcp(toSend.data(listOf("right")).build().toJson())
    }

    companion object {
        private val log = LoggerFactory.getLogger(PacmanGame::class.java)
    }
}