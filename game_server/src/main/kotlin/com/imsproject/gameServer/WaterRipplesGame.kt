package com.imsproject.gameServer

import com.imsproject.common.gameServer.GameAction


class WaterRipplesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor : ClientHandler, action: GameAction) {
        //TODO: Implement game logic

        sendGameAction(action)
    }

}