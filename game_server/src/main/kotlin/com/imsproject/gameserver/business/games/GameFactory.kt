package com.imsproject.gameserver.business.games

import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.business.ClientHandler
import com.sun.source.tree.Tree
import org.springframework.stereotype.Component

@Component
class GameFactory {
    fun get(lobbyId: String, player1Handler: ClientHandler, player2Handler: ClientHandler, gameType: GameType): Game {
        return when (gameType) {
            GameType.RECESS -> RecessGame(lobbyId, player1Handler, player2Handler)
            GameType.WATER_RIPPLES -> WaterRipplesGame(lobbyId, player1Handler, player2Handler)
            GameType.WINE_GLASSES -> WineGlassesGame(lobbyId, player1Handler, player2Handler)
            GameType.FLOUR_MILL -> FlourMillGame(lobbyId, player1Handler, player2Handler)
            GameType.FLOWER_GARDEN -> FlowerGardenGame(lobbyId, player1Handler, player2Handler)
            GameType.PACMAN -> PacmanGame(lobbyId, player1Handler, player2Handler)
            GameType.WAVES -> WavesGame(lobbyId, player1Handler, player2Handler)
            GameType.TREE -> TreeGame(lobbyId, player1Handler, player2Handler)
            else -> throw IllegalArgumentException("Invalid game type")
        }
    }
}