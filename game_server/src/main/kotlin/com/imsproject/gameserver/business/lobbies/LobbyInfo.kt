package com.imsproject.gameserver.business.lobbies

import com.imsproject.common.gameserver.GameType

data class LobbyInfo(
    val lobbyId: String,
    val state: LobbyState,
    val gameType: GameType,
    val gameDuration: Int,
    val syncWindowLength: Long,
    val syncTolerance: Long,
    val players: List<String>,
    val readyStatus: List<Boolean>,
    val hasSessions: Boolean
)

