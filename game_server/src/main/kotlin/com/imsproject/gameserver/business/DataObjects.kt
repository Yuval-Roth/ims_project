package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType

data class Session(
    val sessionId: String,
    val gameType: GameType,
    val duration: Int,
    val syncWindowLength: Long,
    val syncTolerance: Long,
    val isWarmup: Boolean,
    var dbId: Int? = null,
    var state: SessionState = SessionState.NOT_STARTED
)

enum class SessionState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}