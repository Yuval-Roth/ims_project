package com.imsproject.gameserver.dataAccess.models

import com.imsproject.common.gameserver.SessionEvent

data class ParticipantDTO(
    val pid: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class ExperimentDTO(
    val expId: Int?,
    val pid1: Int?,
    val pid2: Int?
)

data class ExperimentFeedbackDTO(
    val expId: Int,
    val pid: Int,
    val question: String,
    val answer: String
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
    fun toNonNullMap(): Map<String, Any> {
        return listOf(
            "session_id" to sessionId,
            "exp_id" to expId,
            "duration" to duration,
            "session_type" to sessionType,
            "session_order" to sessionOrder,
            "tolerance" to tolerance,
            "window_length" to windowLength,
            "state" to state
        ).filter { it.second != null }
            .associate { it.first to it.second!! }
    }

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
    fun toNonNullMap(): Map<String, Any> {
        return listOf(
            "event_id" to eventId,
            "session_id" to sessionId,
            "type" to type,
            "subtype" to subtype,
            "timestamp" to timestamp,
            "actor" to actor,
            "data" to data
        ).filter { it.second != null }
            .associate { it.first to it.second!! }
    }
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

data class ExperimentWithParticipantNamesDTO(
    val expId: Int,
    val participant1Name: String,
    val participant2Name: String
) //todo: add date?