package com.imsproject.gameServer

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest
import java.io.IOException

abstract class Game (val player1 : ClientHandler, val player2 : ClientHandler) {

    abstract fun handleGameAction(actor : ClientHandler, action: GameAction)

    fun startGame() {
        val startMessage = GameRequest.builder(GameRequest.Type.START_GAME)
            .data(listOf(System.currentTimeMillis().toString()))
            .build().toJson()
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