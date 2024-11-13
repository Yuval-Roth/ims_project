package com.imsproject.game_server

import com.imsproject.utils.gameServer.GameAction
import com.imsproject.utils.gameServer.GameRequest
import java.io.IOException

abstract class Game (val player1 : ClientHandler, val player2 : ClientHandler) {

    abstract fun handleGameAction(actor : ClientHandler, action: GameAction)

    fun startGame() {
        val startMessage = GameRequest.builder(GameRequest.Type.START_GAME).build().toJson()
        player1.sendTcp(startMessage)
        player2.sendTcp(startMessage)
    }

    fun endGame() {
        // Send exit message
        val exitMessage = GameRequest.builder(GameRequest.Type.END_GAME).build().toJson()
        try{
            player1.sendTcp(exitMessage)
        } catch (ignored: IOException){ }
        try{
            player2.sendTcp(exitMessage)
        } catch (ignored: IOException){ }
    }

    protected fun sendGameAction(message: GameAction){
        val m = message.toString()
        player1.sendUdp(m)
        player2.sendUdp(m)
    }
}