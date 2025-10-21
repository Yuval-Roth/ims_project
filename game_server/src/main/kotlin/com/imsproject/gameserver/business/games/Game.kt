package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.ClientHandler
import com.imsproject.gameserver.business.TimeServerService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
abstract class Game (
    val lobbyId: String,
    val player1 : ClientHandler,
    val player2 : ClientHandler
) {

    private var player1Ready = false
    private var player2Ready = false
    var localStartTime = -1L
    var timeServerStartTime = -1L

    abstract fun handleGameAction(actor: ClientHandler, action: GameAction)

    fun clientFinishedSetup(client: ClientHandler) {
        when (client.id) {
            player1.id -> player1Ready = true
            player2.id -> player2Ready = true
            else -> throw IllegalArgumentException("Client not part of this game")
        }

        if (player1Ready && player2Ready) {
            val toSend = GameRequest.builder(GameRequest.Type.SESSION_SETUP_COMPLETE).build().toJson()
            GlobalScope.launch(Dispatchers.IO){
                player1.sendTcp(toSend)
            }
            GlobalScope.launch(Dispatchers.IO) {
                player2.sendTcp(toSend)
            }
        }
    }

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