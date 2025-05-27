package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.gameserver.business.ClientHandler
import org.slf4j.LoggerFactory


class FlourMillGame(
    lobbyId: String,
    player1: ClientHandler,
    player2: ClientHandler
) : Game(lobbyId, player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.USER_INPUT -> {
                val otherPlayer = if (actor == player1) player2 else player1
                otherPlayer.sendUdp(action.toString())
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

//    override fun startGame(sessionId: Int) {
//        val timeHandler = TimeServerHandler.instance
//        val timeServerCurr = timeHandler.timeServerCurrentTimeMillis().toString()
//        localStartTime =  System.currentTimeMillis() + timeHandler.timeServerDelta
//        val toSend = GameRequest.builder(GameRequest.Type.START_GAME)
//            .sessionId(sessionId.toString())
//            .timestamp(timeServerCurr)
//        player1.sendTcp(toSend.data(listOf("left")).build().toJson())
//        player2.sendTcp(toSend.data(listOf("right")).build().toJson())
//    }

    companion object {
        private val log = LoggerFactory.getLogger(FlourMillGame::class.java)
    }
}