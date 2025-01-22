package com.imsproject.gameserver.business.lobbies

import com.imsproject.common.gameserver.GameType

class Lobby(
    val id: String,
    var gameType: GameType,
    var gameDuration: Int = -1 // -1 means the game has no time limit
) {

    var player1Id : String? = null
        private set
    var player2Id : String? = null
        private set

    private var player1Ready = false
    private var player2Ready = false

    var state = LobbyState.WAITING

    /**
     * returns true if the player was added successfully, false if the lobby is full
     */
    @Synchronized
    fun add(player: String) : Boolean {
        return when {
            player1Id == null -> {
                player1Id = player
                true
            }
            player2Id == null -> {
                player2Id = player
                true
            }
            else -> false
        }
    }

    /**
     * @return true if the player was removed successfully, false if the player was not in the lobby
     */
    @Synchronized
    fun remove(player: String) : Boolean {
        return when (player) {
            player1Id -> {
                if(player2Id != null){
                    player1Id = player2Id
                    player2Id = null
                } else {
                    player1Id = null
                }
                player1Ready = false
                true
            }
            player2Id -> {
                player2Id = null
                player2Ready = false
                true
            }
            else -> false
        }
    }

    /**
     * @return true if the player was toggled successfully, false if the player was not in the lobby
     */
    @Synchronized
    fun toggleReady(player: String) : Boolean {
        return when (player) {
            player1Id -> {
                player1Ready = !player1Ready
                true
            }
            player2Id -> {
                player2Ready = !player2Ready
                true
            }
            else -> false
        }
    }

    fun isFull() = player1Id != null && player2Id != null

    fun isEmpty() = player1Id == null && player2Id == null

    fun isReady() = isFull() && player1Ready && player2Ready

    operator fun contains(player: String) = player == player1Id || player == player2Id

    fun getPlayers() = setOfNotNull(player1Id, player2Id)

    fun getInfo() : LobbyInfo {
        val players = listOfNotNull(player1Id, player2Id)
        val playersReadyStatus = players.map {
            when (it) {
                player1Id -> player1Ready
                player2Id -> player2Ready
                else -> throw IllegalStateException("Player $it is not in the lobby, but was found in the player list")
            }
        }
        return LobbyInfo(id, gameType, state,gameDuration, players, playersReadyStatus)
    }
}