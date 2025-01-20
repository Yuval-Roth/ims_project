package com.imsproject.gameserver.dataAccess.models

data class Participant(
    val pid: Int?,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val gender: String?,
    val phone: String?,
    val email: String?
)

data class Lobby(
    val lobbyId: Int,
    val pid1: Int?,
    val pid2: Int?
)

data class Session(
    val sessionId: Int,
    val lobbyId: Int,
    val duration: Int?,
    val sessionType: String,
    val sessionOrder: Int
)


data class SessionEvent(
    val eventId: Int,
    val sessionId: Int,
    val type: String,
    val subtype: String,
    val timestamp: Long,
    val actor: String,
    val data: String?
)
