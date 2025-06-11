package com.imsproject.gameserver.dataAccess.models

import com.imsproject.common.gameserver.SessionEvent
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.exp

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
    val pid2: Int?,
    val dateTime: LocalDateTime?
)

data class ExperimentFeedbackDTO(
    val expId: Int?,
    val pid: Int?,
    val question: String?,
    val answer: String?
) {
    fun toNonNullMap(): Map<String, Any> {
        return mutableMapOf<String,Any>().apply {
            expId?.let { this["exp_id"] = it }
            pid?.let { this["pid"] = it }
            question?.let { this["question"] = it }
            answer?.let { this["answer"] = it }
        }
    }
}

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
        return mutableMapOf<String, Any>().apply {
            sessionId?.let { this["session_id"] = it }
            expId?.let { this["exp_id"] = it }
            duration?.let { this["duration"] = it }
            sessionType?.let { this["session_type"] = it }
            sessionOrder?.let { this["session_order"] = it }
            tolerance?.let { this["tolerance"] = it }
            windowLength?.let { this["window_length"] = it }
            state?.let { this["state"] = it }
        }
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
        return mutableMapOf<String, Any>().apply {
            eventId?.let { this["event_id"] = it }
            sessionId?.let { this["session_id"] = it }
            type?.let { this["type"] = it }
            subtype?.let { this["subtype"] = it }
            timestamp?.let { this["timestamp"] = it }
            actor?.let { this["actor"] = it }
            data?.let { this["data"] = it }
        }
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

data class SessionFeedbackDTO(
    val sessionId: Int?,
    val pid: Int?,
    val question: String?,
    val answer: String?
) {
    fun toNonNullMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            sessionId?.let { this["session_id"] = it }
            pid?.let { this["pid"] = it }
            question?.let { this["question"] = it }
            answer?.let { this["answer"] = it }
        }
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
    val dateTime: LocalDateTime,
    val participant1Name: String,
    val participant2Name: String
)