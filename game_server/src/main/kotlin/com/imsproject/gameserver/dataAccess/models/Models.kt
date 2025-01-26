package com.imsproject.gameserver.dataAccess.models

import com.imsproject.common.gameserver.SessionEvent

data class ParticipantDTO(
    val pid: Int?,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val gender: String?,
    val phone: String?,
    val email: String?
)

data class ExperimentDTO(
    val expId: Int?,
    val pid1: Int?,
    val pid2: Int?
)

data class SessionDTO(
    val sessionId: Int? = null,
    val expId: Int? = null,
    val duration: Int? = null,
    val sessionType: String? = null,
    val sessionOrder: Int? = null,
    val tolerance: Int? = null,
    val windowLength: Int? = null,
    val state: String? = null
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
){
    companion object {
        fun fromSessionEvent(event: SessionEvent, sessionId: Int? = null) =
            SessionEventDTO(
                null,
                sessionId,
                event.type.name,
                event.subType.name,
                event.timestamp,
                event.actor,
                event.data
            )
    }
}

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