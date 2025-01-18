package com.imsproject.gameserver.business.lobbies

import com.imsproject.common.gameserver.GameType
import com.imsproject.common.utils.JsonUtils

data class LobbyInfo(
    val lobbyId: String,
    val gameType: GameType,
    val state: LobbyState,
    val gameDuration: Int,
    val players: List<String>,
    val readyStatus: List<Boolean>
){

    fun toJson(): String {
        return JsonUtils.serialize(this)
    }

    companion object {
        fun fromJson(message: String): LobbyInfo {
            return JsonUtils.deserialize(message)
        }
    }
}
