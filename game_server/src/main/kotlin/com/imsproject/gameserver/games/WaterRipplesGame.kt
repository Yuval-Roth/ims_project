package com.imsproject.gameserver.games

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import com.imsproject.gameserver.ClientHandler
import org.slf4j.LoggerFactory


class WaterRipplesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.CLICK -> {
                val toSend = GameAction.builder(GameAction.Type.CLICK)
                    .actor(actor.id)
                    .timestamp(action.timestamp)
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

    companion object {
        private val log = LoggerFactory.getLogger(WaterRipplesGame::class.java)
    }
}