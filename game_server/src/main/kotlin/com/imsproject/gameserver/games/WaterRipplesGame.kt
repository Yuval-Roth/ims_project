package com.imsproject.gameserver.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.gameserver.ClientHandler
import org.slf4j.LoggerFactory


class WaterRipplesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

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

    companion object {
        private val log = LoggerFactory.getLogger(WaterRipplesGame::class.java)
    }
}