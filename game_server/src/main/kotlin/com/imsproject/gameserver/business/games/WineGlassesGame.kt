package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.gameserver.business.ClientHandler
import org.slf4j.LoggerFactory


class WineGlassesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor: ClientHandler, action: GameAction) {
        when(action.type) {
            GameAction.Type.USER_INPUT -> {
                if(actor.id == player1.id) {
                    player2.sendUdp(action.toString())
                } else {
                    player1.sendUdp(action.toString())
                }
            }
            else -> {
                log.debug("Unexpected action type: {}", action.type)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WineGlassesGame::class.java)
    }
}