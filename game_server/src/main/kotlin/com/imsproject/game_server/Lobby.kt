package com.imsproject.game_server

import com.imsproject.utils.gameServer.GameType
import com.imsproject.utils.gameServer.LobbyInfo
import com.imsproject.utils.gameServer.LobbyState

class Lobby(val id : String, val gameType: GameType) {

    var host : ClientHandler? = null
        private set
    var guest : ClientHandler? = null
        private set

    private var hostReady = false
    private var guestReady = false

    var state = LobbyState.WAITING

    /**
     * returns true if the player was added successfully, false if the lobby is full
     */
    @Synchronized
    fun add(player: ClientHandler) : Boolean {
        return when {
            host == null -> {
                host = player
                true
            }
            guest == null -> {
                guest = player
                true
            }
            else -> false
        }
    }

    /**
     * @throws IllegalArgumentException if the player is not in the lobby
     */
    @Synchronized
    fun remove(player: ClientHandler) {
        when (player) {
            host -> {
                if(guest != null){
                    host = guest
                    guest = null
                } else {
                    host = null
                }
            }
            guest -> {
                guest = null
            }
            else -> throw IllegalArgumentException("Player not in the lobby")
        }
        guestReady = false
        hostReady = false
    }

    /**
     * @throws IllegalArgumentException if the player is not in the lobby
     */
    @Synchronized
    fun toggleReady(player: ClientHandler) {
        when (player) {
            host -> {
                hostReady = !hostReady
            }
            guest -> {
                guestReady = !guestReady
            }
            else -> throw IllegalArgumentException("Player not in the lobby")
        }
    }

    fun isFull() = host != null && guest != null

    fun isEmpty() = host == null && guest == null

    fun isReady() = isFull() && hostReady && guestReady

    operator fun contains(player: ClientHandler) = player == host || player == guest

    fun getPlayers() = setOfNotNull(host, guest)

    fun getInfo() : LobbyInfo {
        val ids = getPlayers().map { it.id }
        val players : List<String> = ids.toList()
        return LobbyInfo(id, gameType, state, players)
    }
}