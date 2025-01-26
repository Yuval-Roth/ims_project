package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameAction
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.GameRequest.Type
import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.toJson
import com.imsproject.gameserver.business.games.FlourMillGame
import com.imsproject.gameserver.business.games.Game
import com.imsproject.gameserver.business.games.WaterRipplesGame
import com.imsproject.gameserver.business.games.WineGlassesGame
import com.imsproject.gameserver.business.lobbies.LobbyState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class GameController(
    private val clients: ClientController,
    private val timeServer: TimeServerHandler,
    private val lobbies: LobbyController
) {

    init{
        clients.onClientDisconnect = { onClientDisconnect(it) }
    }

    private val games = ConcurrentHashMap<String, Game>()
    private val clientIdToGame = ConcurrentHashMap<String, Game>()

    /**
     * @throws IllegalArgumentException if anything is wrong with the message
     */
    @Throws(IllegalArgumentException::class)
    fun handleGameAction(clientHandler: ClientHandler, action: GameAction) {
        // ========= parameter validation ========= |
        val game = clientIdToGame[clientHandler.id] ?: run{
            log.debug("handleGameAction: Game not found for client: {} action: {}",clientHandler.id,action)
            throw IllegalArgumentException("Client not in game")
        }
        // ======================================== |

        game.handleGameAction(clientHandler, action)
    }

    fun onClientDisconnect(clientHandler: ClientHandler){
        log.debug("onClientDisconnect() with clientId: {}",clientHandler.id)
        val game = clientIdToGame[clientHandler.id]
        if(game != null){
            log.debug("onClientDisconnect: Player was in game, ending game")
            endGame(game.lobbyId, "Player ${clientHandler.id} disconnected")
        } else  {
            log.debug("onClientDisconnect: Player not in game")
        }
        log.debug("onClientDisconnect() successful")
    }

    fun onClientConnect(client: ClientHandler) {
        log.debug("onClientConnect() with clientId: {}",client.id)

        // check if the client was in a lobby
        val lobby = lobbies.getByClientId(client.id) ?: run {
            // client not in a lobby, nothing to do
            log.debug("onClientConnect: Player not in lobby")
            return
        }
        log.debug("onClientConnect: Player was in lobby: {}, adding player back to lobby",lobby)
        GameRequest.builder(Type.JOIN_LOBBY)
            .lobbyId(lobby.id)
            .gameType(lobby.gameType)
            .build().toJson()
            .also{ client.sendTcp(it) } // notify the client

        // check if the client was in a game
        val game = clientIdToGame[client.id] ?: run {
            // client not in a game, nothing to do
            log.debug("onClientReconnect: Player not in game")
            return
        }
        log.debug("onClientConnect: Player was in game, rejoining player to game")
        GameRequest.builder(Type.RECONNECT_TO_GAME)
            .timestamp(game.startTime.toString())
            .build().toJson()
            .also{ client.sendTcp(it) } // notify the client

        log.debug("onClientConnect() successful")
    }

    fun endGame(lobbyId: String, errorMessage: String? = null) {
        log.debug("endGame() with lobbyId: {}", lobbyId)

        // check if the game exists
        val game = games[lobbyId] ?: run {
            log.debug("endGame: Game not found for lobby {}", lobbyId)
            throw IllegalArgumentException("Game not found for lobby $lobbyId")
        }

        game.endGame(errorMessage) // game.endGame() notifies the clients
        clientIdToGame.remove(game.player1.id)
        clientIdToGame.remove(game.player2.id)
        games.remove(game.lobbyId)
        val lobby = lobbies[lobbyId]
        lobby?.state = LobbyState.WAITING
        lobby?.resetReady()
        
        log.debug("endGame() successful")
    }

    fun startGame(lobbyId: String, sessionId: Int) {
        log.debug("startGame() with lobbyId: {}", lobbyId)

        // Check if the lobby exists
        val lobby = lobbies[lobbyId] ?: run {
            log.debug("startGame: Lobby not found")
            throw IllegalArgumentException("Lobby not found")
        }

        // Check if the lobby is ready
        if(! lobby.isReady()) {
            log.debug("startGame: Lobby is not ready")
            throw IllegalStateException("Lobby is not ready")
        }

        // lobby is ready implies lobby has 2 players
        val player1Id = lobby.player1Id!!
        val player2Id = lobby.player2Id!!

        // ========= player validation ========= |
        val notFound = mutableListOf<String>()
        val errorMsg2 = "The following were not found: "
        /*(1)*/ val player1Handler = clients.getByClientId(player1Id) ?: run{ notFound.add("Player 1 handler") ; null }
        /*(2)*/ val player2Handler = clients.getByClientId(player2Id) ?: run{ notFound.add("Player 2 handler") ; null }
        if(player1Handler == null || player2Handler == null){
            // should not happen
            log.error("startGame: {} {}",errorMsg2, notFound.joinToString())
            throw IllegalStateException("Failed to start game")
        }
        // ===================================== |

        val game = when(lobby.gameType){
                GameType.WATER_RIPPLES -> {
                    log.debug("startGame: Selected WaterRipplesGame")
                    WaterRipplesGame(lobbyId,player1Handler, player2Handler)
                }
                GameType.WINE_GLASSES -> {
                    log.debug("startGame: Selected WineGlassesGame")
                    WineGlassesGame(lobbyId,player1Handler, player2Handler)
                }
                GameType.FLOUR_MILL -> {
                    log.debug("startGame: Selected FlourMillGame")
                    FlourMillGame(lobbyId,player1Handler, player2Handler)
                }
                else -> {
                    log.debug("startGame: Invalid game type")
                    throw IllegalArgumentException("Invalid game type")
                }
        }
        lobby.state = LobbyState.PLAYING
        games[lobby.id] = game
        clientIdToGame[player1Id] = game
        clientIdToGame[player2Id] = game

        // game.startGame() notifies the clients
        game.startGame(timeServer.timeServerCurrentTimeMillis(), sessionId)

        log.debug("startGame() successful")
    }

    fun getLobbiesWithRunningGames() : List<String> {
        return games.keys.toList()
    }

    companion object{
        private val log = LoggerFactory.getLogger(GameController::class.java)
    }
}
