package com.imsproject.gameserver.dataAccess.models

// change names to _DTO

data class Participant(
    val pid: Int?,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val gender: String?,
    val phone: String?,
    val email: String?
)

data class Experiment( // CHANGE TO EXPERIMENT
    val expId: Int?,
    val pid1: Int?,
    val pid2: Int?
)

data class Session(
    val sessionId: Int?,
    val expId: Int?,
    val duration: Int?,
    val sessionType: String?,
    val sessionOrder: Int?,
    val tolerance: Int?,
    val windowLength: Int?
) // add a companion object for static func of factory creation -> will receive all fields without ids


data class SessionEvent(
    val eventId: Int?,
    val sessionId: Int?,
    val type: String?,
    val subtype: String?,
    val timestamp: Long?,
    val actor: String?,
    val data: String?
)

//todo: expose this to yuval , and then build my dtos from that
data class experimentWithSessions(
    val pid1: Int?,
    val pid2: Int?,


)