package com.imsproject.game_server

import com.imsproject.utils.gameServer.GameAction

class PocGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor : ClientHandler, action: GameAction) {
        sendGameAction(action)
    }
}