package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

data class SessionEvent internal constructor (
    val type: Type,
    val subType: SubType,
    val timestamp: Long,
    val actor: String,
    val data: String? = null
) : Comparable<SessionEvent> {

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
        @SerializedName("angle")                    ANGLE,
        @SerializedName("rotation")                 ROTATION,

        // SENSOR_DATA
        @SerializedName("heart_rate")               HEART_RATE,
        @SerializedName("heart_rate_variability")   HEART_RATE_VARIABILITY,
        @SerializedName("blood_oxygen")             BLOOD_OXYGEN,
        @SerializedName("gyroscope")                GYROSCOPE,
        @SerializedName("accelerometer")            ACCELEROMETER,

        // NETWORK_DATA
        @SerializedName("latency")                  LATENCY,
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

        ;

        override fun toString(): String {
            return name.lowercase()
        }
    }

    fun toJson(): String = JsonUtils.serialize(this)

    override fun compareTo(other: SessionEvent): Int {
        return timestamp.compareTo(other.timestamp)
    }

    // ================================================================================ |
    // ============================ FACTORY METHODS =================================== |
    // ================================================================================ |

    companion object{

        fun fromJson(json: String): SessionEvent = JsonUtils.deserialize(json)

        // ==================== USER_INPUT ==================== |

        fun click(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.USER_INPUT, SubType.CLICK, timestamp, actor)

        fun angle(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, SubType.ANGLE, timestamp, actor, data)

        fun rotation(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, SubType.ROTATION, timestamp, actor, data)

        // ==================== SENSOR_DATA ==================== |

        fun heartRate(
            actor: String,
            timestamp: Long,
            value: Int
        ) = SessionEvent(Type.SENSOR_DATA, SubType.HEART_RATE, timestamp, actor, value.toString())

        fun heartRateVariability(
            actor: String,
            timestamp: Long,
            value: Int
        ) = SessionEvent(Type.SENSOR_DATA, SubType.HEART_RATE_VARIABILITY, timestamp, actor, value.toString())

        fun bloodOxygen(
            actor: String,
            timestamp: Long,
            value: Int
        ) = SessionEvent(Type.SENSOR_DATA, SubType.BLOOD_OXYGEN, timestamp, actor, value.toString())

        fun gyroscope(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, SubType.GYROSCOPE, timestamp, actor, data)

        fun accelerometer(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.SENSOR_DATA, SubType.ACCELEROMETER, timestamp, actor, data)

        // ==================== NETWORK_DATA ==================== |

        fun latency(
            actor: String,
            timestamp: Long,
            value: Long
        ) = SessionEvent(Type.NETWORK_DATA, SubType.LATENCY, timestamp, actor, value.toString())

        fun packetOutOfOrder(
            actor: String,
            timestamp: Long,
        ) = SessionEvent(Type.NETWORK_DATA, SubType.PACKET_OUT_OF_ORDER, timestamp, actor)

        fun networkError(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.NETWORK_DATA, SubType.NETWORK_ERROR, timestamp, actor, data)

        fun reconnected(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.NETWORK_DATA, SubType.RECONNECTED, timestamp, actor)

        // ==================== SYNC_DATA ==================== |

        fun syncStartTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, SubType.SYNC_START_TIME, timestamp, actor)

        fun syncEndTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, SubType.SYNC_END_TIME, timestamp, actor)

        fun syncedAtTime(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.SYNC_DATA, SubType.SYNCED_AT_TIME, timestamp, actor)

        // ==================== META_DATA ==================== |

        fun serverStartTime(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, SubType.SERVER_START_TIME, timestamp, actor, data)

        fun clientStartTime(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, SubType.CLIENT_START_TIME, timestamp, actor, data)

        fun timeServerDelta(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.META_DATA, SubType.TIME_SERVER_DELTA, timestamp, actor, data)

        fun sessionStarted(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.META_DATA, SubType.SESSION_STARTED, timestamp, actor)

        fun sessionEnded(
            actor: String,
            timestamp: Long,
            reason: String
        ) = SessionEvent(Type.META_DATA, SubType.SESSION_ENDED, timestamp, actor, reason)
    }
}
