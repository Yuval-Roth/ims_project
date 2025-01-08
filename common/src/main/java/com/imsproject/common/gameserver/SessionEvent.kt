package com.imsproject.common.gameserver

import com.imsproject.common.utils.JsonUtils

data class SessionEvent (
    val type: Type,
    val subType: SubType,
    val timestamp: Long,
    val actor: String,
    val data: String? = null
) : Comparable<SessionEvent> {
    enum class Type{
        USER_INPUT,
        SENSOR_DATA,
        NETWORK_DATA,
        SYNC_DATA
    }

    enum class SubType {
        // USER_INPUT
        CLICK,
        POSITION,

        // SENSOR_DATA
        HEART_RATE,
        HEART_RATE_VARIABILITY,
        BLOOD_OXYGEN,
        GYROSCOPE,
        ACCELEROMETER,

        // NETWORK_DATA
        LATENCY,
        PACKET_OUT_OF_ORDER,
        TIMEOUT,

        // SYNC_DATA
        SYNC_START_TIME,
        SYNC_END_TIME,
        SYNCED_AT_TIME
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

        fun position(
            actor: String,
            timestamp: Long,
            data: String
        ) = SessionEvent(Type.USER_INPUT, SubType.POSITION, timestamp, actor, data)

        fun click(
            actor: String,
            timestamp: Long
        ) = SessionEvent(Type.USER_INPUT, SubType.CLICK, timestamp, actor)

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
