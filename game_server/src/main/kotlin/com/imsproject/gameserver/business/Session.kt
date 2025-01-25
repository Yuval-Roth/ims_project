package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType

data class Session(
    val sessionId: String,
    val gameType: GameType,
    val duration: Int,
    val syncWindowLength: Long,
    val syncTolerance: Long,
    var dbId: Int? = null,
    var state: SessionState = SessionState.NOT_STARTED
)