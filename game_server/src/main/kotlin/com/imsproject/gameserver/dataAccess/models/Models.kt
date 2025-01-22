package com.imsproject.gameserver.dataAccess.models

// change names to _DTO

data class ParticipantDTO(
    val pid: Int?,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val gender: String?,
    val phone: String?,
    val email: String?
)

data class ExperimentDTO( // CHANGE TO EXPERIMENT
    val expId: Int?,
    val pid1: Int?,
    val pid2: Int?
)

data class SessionDTO(
    val sessionId: Int?,
    val expId: Int?,
    val duration: Int?,
    val sessionType: String?,
    val sessionOrder: Int?,
    val tolerance: Int?,
    val windowLength: Int?
) {
    companion object {
        fun create(expId: Int, sessionData: SessionData) =
            SessionDTO(null, expId, sessionData.duration, sessionData.sessionType, sessionData.sessionOrder, sessionData.tolerance, sessionData.windowLength)
    }
}
data class SessionEventDTO(
    val eventId: Int?,
    val sessionId: Int?,
    val type: String?,
    val subtype: String?,
    val timestamp: Long?,
    val actor: String?,
    val data: String?
)

//todo: expose this to yuval , and then build my dtos from that
data class ExpWithSessionsData(
    val pid1: Int,
    val pid2: Int,
    val sessions: List<SessionData>
)

data class SessionData(
    val duration: Int,
    val sessionType: String,
    val sessionOrder: Int,
    val tolerance: Int,
    val windowLength: Int
)