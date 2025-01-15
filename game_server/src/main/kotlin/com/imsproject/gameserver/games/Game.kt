package com.imsproject.gameserver.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.gameserver.ClientHandler
import kotlinx.coroutines.*

abstract class Game (val player1 : ClientHandler, val player2 : ClientHandler) {

    abstract fun handleGameAction(actor: ClientHandler, action: GameAction)
    var startTime: Long = -1
    private var timeLimitJob: Job? = null

    open fun startGame(timestamp: Long) {
        startTime = timestamp
        val startMessage = GameRequest.builder(GameRequest.Type.START_GAME)
            .timestamp(timestamp.toString())
            .build().toJson()
        player1.sendTcp(startMessage)
        player2.sendTcp(startMessage)
    }

    fun endGame(errorMessage: String? = null) {
        timeLimitJob?.cancel()
        // Send exit message
        val exitMessage = GameRequest.builder(GameRequest.Type.END_GAME)
            .apply { errorMessage?.let { message(it) } }
            .success(errorMessage == null)
            .build().toJson()
        try{
            player1.sendTcp(exitMessage)
        } catch (ignored: Exception){ }
        try{
            player2.sendTcp(exitMessage)
        } catch (ignored: Exception){ }
    }

    fun setTimeLimit(seconds: Int){
        timeLimitJob?.cancel()

        if (seconds > 0){
            @OptIn(DelicateCoroutinesApi::class)
            timeLimitJob = GlobalScope.launch {
                delay(seconds * 1000L)
                endGame()
            }
        }
    }

    protected open fun sendGameAction(message: GameAction){
        val m = message.toString()
        player1.sendUdp(m)
        player2.sendUdp(m)
    }
}