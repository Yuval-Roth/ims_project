package com.imsproject.gameserver.business.lobbies

import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.business.Session

class Lobby(val id: String) {

    var player1Id : String? = null
        private set
    var player2Id : String? = null
        private set

    private var player1Ready = false
    private var player2Ready = false

    var gameType: GameType = GameType.UNDEFINED
        private set
    var gameDuration: Int = -1
        private set
    var syncTolerance: Long = -1
        private set
    var syncWindowLength: Long = -1
        private set
    var isWarmup = false
        private set
    var state = LobbyState.WAITING
    var hasSessions = false
    var experimentRunning = false
    var expId: Int? = null

    fun configure(sessionDetails: Session) {
        gameType = sessionDetails.gameType
        gameDuration = sessionDetails.duration
        syncWindowLength = sessionDetails.syncWindowLength
        syncTolerance = sessionDetails.syncTolerance
        isWarmup = sessionDetails.isWarmup
    }

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

    fun resetReady(){
        player1Ready = false
        player2Ready = false
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
        return LobbyInfo(
            id,
            state,
            gameType,
            gameDuration,
            syncWindowLength,
            syncTolerance,
            players,
            playersReadyStatus,
            hasSessions,
            experimentRunning
        )
    }
}