package com.imsproject.gameserver.business

import com.imsproject.common.gameserver.GameType

//data class Participant(
//    val pid: String?,
//    val firstName: String?,
//    val lastName: String?,
//    val age: Int?,
//    val gender: String?,
//    val phone: String?,
//    val email: String?
//)

data class Session(
    val sessionId: String,
    val gameType: GameType,
    val duration: Int,
    val syncWindowLength: Long,
    val syncTolerance: Long,
    var dbId: Int? = null,
    var state: SessionState = SessionState.NOT_STARTED
)

enum class SessionState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}