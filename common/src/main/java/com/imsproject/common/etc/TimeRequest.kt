package com.imsproject.common.etc

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils
import java.time.LocalDateTime
import java.time.ZoneId

class TimeRequest private constructor(
    val type: Type?,
    val time: Long?
) {
    enum class Type {
        @SerializedName("current_time_Millis") CURRENT_TIME_MILLIS,
        @SerializedName("nano_time") NANO_TIME,
    }

    fun toJson() = JsonUtils.serialize(this)

    companion object {
        fun currentTimeMillis() = TimeRequest(
            Type.CURRENT_TIME_MILLIS,
            LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        fun nanoTime() = TimeRequest(Type.NANO_TIME, System.nanoTime())
        fun request(type: Type) = TimeRequest(type, null)
        fun fromJson(json: String) : TimeRequest = JsonUtils.deserialize(json)
    }
}