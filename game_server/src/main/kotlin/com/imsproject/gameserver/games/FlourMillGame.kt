package com.imsproject.gameserver.games

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.gameserver.ClientHandler
import org.slf4j.LoggerFactory


class FlourMillGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.POSITION -> {
                val toSend = GameAction.builder(GameAction.Type.POSITION)
                    .actor(actor.id)
                    .timestamp(action.timestamp)
                    .data(action.data)
                    // add more things if needed
                    .build()
                sendGameAction(toSend)
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

    override fun handleGameRequest(actor: ClientHandler, request: GameRequest) {
        when(request.type) {
            else -> {
                log.debug("Unexpected request type: {}", request.type)
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