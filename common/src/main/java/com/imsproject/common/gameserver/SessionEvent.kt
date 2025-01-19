package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

data class SessionEvent (
    val sessionId: String,
    val type: Type,
    val subType: SubType,
    val timestamp: Long,
    val actor: String,
    val data: String? = null
) : Comparable<SessionEvent> {

    private data class CompressedSessionEvent(
        val sid: String, // sessionId
        val to: Int, // type ordinal
        val sto: Int, // subType ordinal
        val ts: Long, // timestamp
        val a: String, // actor
        val d: String? // data
    )

    enum class Type {
        @SerializedName("user_input")               USER_INPUT,
        @SerializedName("sensor_data")              SENSOR_DATA,
        @SerializedName("network_data")             NETWORK_DATA,
        @SerializedName("sync_data")                SYNC_DATA,
        @SerializedName("meta_data")                META_DATA,

        ;

        override fun toString(): String {
            return name.lowercase()
        }
    }

    enum class SubType {
        // USER_INPUT
        @SerializedName("click")                    CLICK,
        @SerializedName("opponent_click")           OPPONENT_CLICK,
        @SerializedName("angle")                    ANGLE,
        @SerializedName("opponent_angle")           OPPONENT_ANGLE,
        @SerializedName("rotation")                 ROTATION,
        @SerializedName("opponent_rotation")        OPPONENT_ROTATION,
        @SerializedName("frequency")                FREQUENCY,
        @SerializedName("opponent_frequency")       OPPONENT_FREQUENCY,

        // SENSOR_DATA
        @SerializedName("heart_rate")               HEART_RATE,
        @SerializedName("heart_rate_variability")   HEART_RATE_VARIABILITY,
        @SerializedName("blood_oxygen")             BLOOD_OXYGEN,
        @SerializedName("gyroscope")                GYROSCOPE,
        @SerializedName("accelerometer")            ACCELEROMETER,

        // NETWORK_DATA
        @SerializedName("latency")                  LATENCY,
        @SerializedName("timed_out")                TIMED_OUT,
        @SerializedName("average_latency")          AVERAGE_LATENCY,
        @SerializedName("min_latency")              MIN_LATENCY,
        @SerializedName("max_latency")              MAX_LATENCY,
        @SerializedName("jitter")                   JITTER,
        @SerializedName("median_latency")           MEDIAN_LATENCY,
        @SerializedName("measurement_count")        MEASUREMENT_COUNT,
        @SerializedName("timeout_threshold")        TIMEOUT_THRESHOLD,
        @SerializedName("timeouts_count")           TIMEOUTS_COUNT,
        @SerializedName("packet_out_of_order")      PACKET_OUT_OF_ORDER,
        @SerializedName("network_error")            NETWORK_ERROR,
        @SerializedName("reconnected")              RECONNECTED,

        // SYNC_DATA
        @SerializedName("sync_start_time")          SYNC_START_TIME,
        @SerializedName("sync_end_time")            SYNC_END_TIME,
        @SerializedName("synced_at_time")           SYNCED_AT_TIME,

        // META_DATA
        @SerializedName("server_start_time")        SERVER_START_TIME,
        @SerializedName("client_start_time")        CLIENT_START_TIME,
        @SerializedName("time_server_delta")        TIME_SERVER_DELTA,
        @SerializedName("session_started")          SESSION_STARTED,
        @SerializedName("session_ended")            SESSION_ENDED,
        @SerializedName("sync_tolerance")           SYNC_TOLERANCE,
        @SerializedName("sync_window_length")       SYNC_WINDOW_LENGTH,

        ;

        override fun toString(): String {
            return name.lowercase()
        }
    }

    fun toJson(): String = JsonUtils.serialize(this)

    fun toCompressedJson(): String = CompressedSessionEvent(
        sessionId,
        type.ordinal,
        subType.ordinal,
        timestamp,
        actor,
        data
    ).let{ JsonUtils.serialize(it) }

    override fun compareTo(other: SessionEvent): Int {
        return timestamp.compareTo(other.timestamp)
    }

    // ================================================================================ |
    // ============================ FACTORY METHODS =================================== |
    // ================================================================================ |

    companion object{

        fun fromCompressedJson(json: String): SessionEvent = JsonUtils.deserialize<CompressedSessionEvent>(json)
            .let {
                SessionEvent(
                    it.sid,
                    Type.entries[it.to],
                    SubType.entries[it.sto],
                    it.ts,
                    it.a,
                    it.d
                )
            }

        fun fromJson(json: String): SessionEvent = JsonUtils.deserialize(json)

        // ==================== USER_INPUT ==================== |

        fun click(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.CLICK, timestamp, actor)

        fun opponentClick(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.OPPONENT_CLICK, timestamp, actor)

        fun angle(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.ANGLE, timestamp, actor, data)

        fun opponentAngle(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.OPPONENT_ANGLE, timestamp, actor, data)

        fun rotation(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.ROTATION, timestamp, actor, data)

        fun opponentRotation(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.OPPONENT_ROTATION, timestamp, actor, data)

        fun frequency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.FREQUENCY, timestamp, actor, data)

        fun opponentFrequency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.USER_INPUT, SubType.OPPONENT_FREQUENCY, timestamp, actor, data)

        // ==================== SENSOR_DATA ==================== |

        fun heartRate(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.SENSOR_DATA, SubType.HEART_RATE, timestamp, actor, data)

        fun heartRateVariability(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.SENSOR_DATA, SubType.HEART_RATE_VARIABILITY, timestamp, actor, data)

        fun bloodOxygen(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.SENSOR_DATA, SubType.BLOOD_OXYGEN, timestamp, actor, data)

        fun gyroscope(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.SENSOR_DATA, SubType.GYROSCOPE, timestamp, actor, data)

        fun accelerometer(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.SENSOR_DATA, SubType.ACCELEROMETER, timestamp, actor, data)

        // ==================== NETWORK_DATA ==================== |

        fun latency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.LATENCY, timestamp, actor, data)

        fun timedOut(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.TIMED_OUT, timestamp, actor)

        fun averageLatency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.AVERAGE_LATENCY, timestamp, actor, data)

        fun minLatency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.MIN_LATENCY, timestamp, actor, data)

        fun maxLatency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.MAX_LATENCY, timestamp, actor, data)

        fun jitter(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.JITTER, timestamp, actor, data)

        fun medianLatency(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.MEDIAN_LATENCY, timestamp, actor, data)

        fun measurementCount(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.MEASUREMENT_COUNT, timestamp, actor, data)

        fun timeoutThreshold(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.TIMEOUT_THRESHOLD, timestamp, actor, data)

        fun timeoutsCount(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.TIMEOUTS_COUNT, timestamp, actor, data)

        fun packetOutOfOrder(
            sessionId: String,
            actor: String,
            timestamp: Long,
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.PACKET_OUT_OF_ORDER, timestamp, actor)

        fun networkError(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.NETWORK_ERROR, timestamp, actor, data)

        fun reconnected(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.NETWORK_DATA, SubType.RECONNECTED, timestamp, actor)

        // ==================== SYNC_DATA ==================== |

        fun syncStartTime(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.SYNC_DATA, SubType.SYNC_START_TIME, timestamp, actor)

        fun syncEndTime(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.SYNC_DATA, SubType.SYNC_END_TIME, timestamp, actor)

        fun syncedAtTime(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.SYNC_DATA, SubType.SYNCED_AT_TIME, timestamp, actor)

        // ==================== META_DATA ==================== |

        fun serverStartTime(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.SERVER_START_TIME, timestamp, actor, data)

        fun clientStartTime(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.CLIENT_START_TIME, timestamp, actor, data)

        fun timeServerDelta(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.TIME_SERVER_DELTA, timestamp, actor, data)

        fun sessionStarted(
            sessionId: String,
            actor: String,
            timestamp: Long
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.SESSION_STARTED, timestamp, actor)

        fun sessionEnded(
            sessionId: String,
            actor: String,
            timestamp: Long,
            reason: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.SESSION_ENDED, timestamp, actor, reason)

        fun syncTolerance(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.SYNC_TOLERANCE, timestamp, actor, data)

        fun syncWindowLength(
            sessionId: String,
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(sessionId, Type.META_DATA, SubType.SYNC_WINDOW_LENGTH, timestamp, actor, data)

    }
}
