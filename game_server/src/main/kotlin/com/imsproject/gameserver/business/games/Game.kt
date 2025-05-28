package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.TimeServerService

abstract class Game (
    val lobbyId: String,
    val player1 : ClientHandler,
    val player2 : ClientHandler
) {

    abstract fun handleGameAction(actor: ClientHandler, action: GameAction)
    var localStartTime = -1L
    var timeServerStartTime = -1L

    open fun startGame(sessionId: Int) {
        val timeHandler = TimeServerService.instance
        timeServerStartTime = timeHandler.timeServerCurrentTimeMillis()
        localStartTime =  System.currentTimeMillis() + timeHandler.timeServerDelta
        val startMessage = GameRequest.builder(GameRequest.Type.START_GAME)
            .timestamp(timeServerStartTime.toString())
            .sessionId(sessionId.toString())
            .build().toJson()
        player1.sendTcp(startMessage)
        player2.sendTcp(startMessage)
    }

    open fun endGame(expId: Int? = null, errorMessage: String? = null) {
        // Send exit message
        val exitMessage = GameRequest.builder(GameRequest.Type.END_GAME)
            .apply {
                errorMessage?.also { message(it) }
                expId?.also { data(listOf(it.toString())) }
            }
            .success(errorMessage == null)
            .build().toJson()
        try{
            player1.sendTcp(exitMessage)
        } catch (ignored: Exception){ }
        try{
            player2.sendTcp(exitMessage)
        } catch (ignored: Exception){ }
    }

    protected open fun sendGameAction(message: GameAction){
        val m = message.toString()
        player1.sendUdp(m)
        player2.sendUdp(m)
    }
}