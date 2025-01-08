package com.imsproject.gameserver.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.gameserver.ClientHandler
import org.slf4j.LoggerFactory


class WineGlassesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

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

    override fun sendGameAction(message: GameAction) {
        val actor = message.actor ?: run {
            log.error("No actor in message: $message")
            return
        }
        if(actor == player1.id) {
            player2.sendUdp(message.toString())
        } else {
            player1.sendUdp(message.toString())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WineGlassesGame::class.java)
    }
}