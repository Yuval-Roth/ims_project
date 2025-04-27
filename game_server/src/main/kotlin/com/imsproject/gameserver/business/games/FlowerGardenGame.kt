package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.gameserver.business.ClientHandler
import org.slf4j.LoggerFactory


class FlowerGardenGame(
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

    companion object {
        private val log = LoggerFactory.getLogger(FlowerGardenGame::class.java)
    }
}