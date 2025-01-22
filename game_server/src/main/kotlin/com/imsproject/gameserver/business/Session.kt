package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType

data class Session (
    val sessionId: String,
    val duration: Int,
    val gameType: GameType,
    val syncWindowLength: Long,
    val syncTolerance: Long
)