package com.imsproject.common.dataAccess.models

data class Participant(
    val pid: Int,
    val firstName: String,
    val lastName: String,
    val age: Int?,
    val gender: GenderEnum?,
    val phone: String?,
    val email: String
)

enum class GenderEnum {
    Male, Female
}

data class Lobby(
    val lobbyId: Int,
    val pid1: Int?,
    val pid2: Int?
)

data class Session(
    val sessionId: Int,
    val lobbyId: Int,
    val duration: Int?,
    val sessionType: SessionTypeEnum,
    val sessionOrder: Int
)

enum class SessionTypeEnum {
    user_input, sensor_data, network_data, sync_data, meta_data
}

data class SessionEvent(
    val eventId: Int,
    val sessionId: Int,
    val type: SessionTypeEnum,
    val subtype: SessionSubtypeEnum,
    val timestamp: Long,
    val actor: String,
    val data: String?
)

enum class SessionSubtypeEnum {
    click, angle, rotation,
    heart_rate, heart_rate_variability, blood_oxygen, gyroscope, accelerometer,
    latency, packet_out_of_order, timeout,
    sync_start_time, sync_end_time, synced_at_time,
    server_start_time, client_start_time, time_server_delta, session_started, session_ended
}
