package com.imsproject.utils.gameServer

import com.imsproject.utils.JsonUtils

data class LobbyInfo(val lobbyId: String, val gameType: GameType, val state: LobbyState, val players: List<String>){

    fun toJson(): String {
        return JsonUtils.serialize(this)
    }

    companion object {
        fun fromJson(message: String): LobbyInfo {
            return JsonUtils.deserialize(message, this::class.java)
        }
    }
}
