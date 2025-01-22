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
)

