package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

data class SessionEvent (
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
        @SerializedName("sync_data")                SYNC_DATA

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
        @SerializedName("timeout")                  TIMEOUT,

        // SYNC_DATA
        @SerializedName("sync_start_time")          SYNC_START_TIME,
        @SerializedName("sync_end_time")            SYNC_END_TIME,
        @SerializedName("synced_at_time")           SYNCED_AT_TIME

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

        fun timeout(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.NETWORK_DATA, SubType.TIMEOUT, timestamp, actor)

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
    }
}
