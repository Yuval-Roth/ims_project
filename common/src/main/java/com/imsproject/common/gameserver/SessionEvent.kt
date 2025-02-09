package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson

data class SessionEvent internal constructor (
    val type: Type,
    val subType: Subtype,
    val timestamp: Long,
    val actor: String,
    val data: String? = null
) : Comparable<SessionEvent> {

    private data class CompressedSessionEvent(
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
    }

    enum class Subtype {
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
        @SerializedName("inter_beat_interval")      INTER_BEAT_INTERVAL,
        @SerializedName("orientation_azimuth")      ORIENTATION_AZIMUTH,
        @SerializedName("orientation_pitch")        ORIENTATION_PITCH,
        @SerializedName("orientation_roll")         ORIENTATION_ROLL,
        @SerializedName("accelerometer_x")          ACCELEROMETER_X,
        @SerializedName("accelerometer_y")          ACCELEROMETER_Y,
        @SerializedName("accelerometer_z")          ACCELEROMETER_Z,

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
    }

    fun toCompressedJson(): String = CompressedSessionEvent(
        type.ordinal,
        subType.ordinal,
        timestamp,
        actor,
        data
    ).toJson()

    override fun compareTo(other: SessionEvent): Int {
        return timestamp.compareTo(other.timestamp)
    }

    // ================================================================================ |
    // ============================ FACTORY METHODS =================================== |
    // ================================================================================ |

    companion object{

        fun fromCompressedJson(json: String): SessionEvent = fromJson<CompressedSessionEvent>(json)
            .let {
                SessionEvent(
                    Type.entries[it.to],
                    Subtype.entries[it.sto],
                    it.ts,
                    it.a,
                    it.d
                )
            }

        // ==================== USER_INPUT ==================== |

        fun click(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.USER_INPUT, Subtype.CLICK, timestamp, actor)

        fun opponentClick(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.USER_INPUT, Subtype.OPPONENT_CLICK, timestamp, actor)

        fun angle(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.ANGLE, timestamp, actor, data)

        fun opponentAngle(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.OPPONENT_ANGLE, timestamp, actor, data)

        fun rotation(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.ROTATION, timestamp, actor, data)

        fun opponentRotation(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.OPPONENT_ROTATION, timestamp, actor, data)

        fun frequency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.FREQUENCY, timestamp, actor, data)

        fun opponentFrequency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, Subtype.OPPONENT_FREQUENCY, timestamp, actor, data)

        // ==================== SENSOR_DATA ==================== |

        fun heartRate(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.HEART_RATE, timestamp, actor, data)

        fun interBeatInterval(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.INTER_BEAT_INTERVAL, timestamp, actor, data)

        fun orientationAzimuth(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ORIENTATION_AZIMUTH, timestamp, actor, data)

        fun orientationPitch(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ORIENTATION_PITCH, timestamp, actor, data)

        fun orientationRoll(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ORIENTATION_ROLL , timestamp, actor, data)

        fun accelerometerX(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ACCELEROMETER_X, timestamp, actor, data)

        fun accelerometerY(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ACCELEROMETER_Y, timestamp, actor, data)

        fun accelerometerZ(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, Subtype.ACCELEROMETER_Z, timestamp, actor, data)

        // ==================== NETWORK_DATA ==================== |

        fun latency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.LATENCY, timestamp, actor, data)

        fun timedOut(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.TIMED_OUT, timestamp, actor, data)

        fun averageLatency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.AVERAGE_LATENCY, timestamp, actor, data)

        fun minLatency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.MIN_LATENCY, timestamp, actor, data)

        fun maxLatency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.MAX_LATENCY, timestamp, actor, data)

        fun jitter(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.JITTER, timestamp, actor, data)

        fun medianLatency(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.MEDIAN_LATENCY, timestamp, actor, data)

        fun measurementCount(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.MEASUREMENT_COUNT, timestamp, actor, data)

        fun timeoutThreshold(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.TIMEOUT_THRESHOLD, timestamp, actor, data)

        fun timeoutsCount(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.TIMEOUTS_COUNT, timestamp, actor, data)

        fun packetOutOfOrder(
            actor: String,
            timestamp: Long,
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.PACKET_OUT_OF_ORDER, timestamp, actor)

        fun networkError(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.NETWORK_ERROR, timestamp, actor, data)

        fun reconnected(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.NETWORK_DATA, Subtype.RECONNECTED, timestamp, actor)

        // ==================== SYNC_DATA ==================== |

        fun syncStartTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, Subtype.SYNC_START_TIME, timestamp, actor)

        fun syncEndTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, Subtype.SYNC_END_TIME, timestamp, actor)

        fun syncedAtTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, Subtype.SYNCED_AT_TIME, timestamp, actor)

        // ==================== META_DATA ==================== |

        fun serverStartTime(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, Subtype.SERVER_START_TIME, timestamp, actor, data)

        fun clientStartTime(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, Subtype.CLIENT_START_TIME, timestamp, actor, data)

        fun timeServerDelta(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, Subtype.TIME_SERVER_DELTA, timestamp, actor, data)

        fun sessionStarted(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.META_DATA, Subtype.SESSION_STARTED, timestamp, actor)

        fun sessionEnded(
            actor: String,
            timestamp: Long,
            reason: String
        ) = SessionEvent(Type.META_DATA, Subtype.SESSION_ENDED, timestamp, actor, reason)

        fun syncTolerance(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, Subtype.SYNC_TOLERANCE, timestamp, actor, data)

        fun syncWindowLength(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, Subtype.SYNC_WINDOW_LENGTH, timestamp, actor, data)

    }
}
