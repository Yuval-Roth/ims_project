package com.imsproject.gameServer

import com.imsproject.common.gameServer.GameAction
import com.imsproject.common.gameServer.GameRequest


class WaterRipplesGame (player1 : ClientHandler, player2 : ClientHandler) : Game(player1, player2) {

    override fun handleGameAction(actor : ClientHandler, action: GameAction) {
        //TODO: Implement game logic

        val toSend = GameAction.builder(GameAction.Type.CLICK)
            .actor(actor.id)
            .timestamp(action.timestamp)
            // add more things if needed
            .build()
        sendGameAction(toSend)
    }

    override fun handleGameRequest(actor: ClientHandler, request: GameRequest) {
        when(request.type) {
            GameRequest.Type.SYNC_TIME -> {
                val otherPlayer = if(actor == player1) player2 else player1
                otherPlayer.sendTcp(request.toJson())
            }
            else -> {
                //TODO: implement error handling
            }
        }
    }
}