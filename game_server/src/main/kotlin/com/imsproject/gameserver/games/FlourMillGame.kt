package com.imsproject.gameserver.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.gameserver.ClientHandler
import org.slf4j.LoggerFactory


class FlourMillGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.POSITION -> {
                sendGameAction(action)
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

    override fun startGame(timestamp: Long) {
        val toSend = GameRequest.builder(GameRequest.Type.START_GAME)
            .timestamp(timestamp.toString())
        player1.sendTcp(toSend.data(listOf("left")).build().toJson())
        player2.sendTcp(toSend.data(listOf("right")).build().toJson())
    }

    companion object {
        private val log = LoggerFactory.getLogger(FlourMillGame::class.java)
    }
}