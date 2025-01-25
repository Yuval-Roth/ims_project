package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.toJson

data class GameRequest internal constructor(
    val type: Type,
    val playerId: String?,
    val lobbyId : String?,
    val gameType : GameType?,
    val success : Boolean?,
    val message : String?,
    val duration: Int?,
    val sessionId: String?,
    val sessionIds: List<String>?,
    val data : List<String>?,
    val timestamp: String?,
    val syncWindowLength: Long?,
    val syncTolerance: Long?,
){
    enum class Type {
        @SerializedName("ping")                     PING,
        @SerializedName("pong")                     PONG,
        @SerializedName("enter")                    ENTER,
        @SerializedName("force_enter")              FORCE_ENTER,
        @SerializedName("already_connected")        ALREADY_CONNECTED,
        @SerializedName("reconnect")                RECONNECT,
        @SerializedName("exit")                     EXIT,
        @SerializedName("get_online_player_ids")    GET_ONLINE_PLAYER_IDS,
        @SerializedName("get_lobbies")              GET_ALL_LOBBIES,
        @SerializedName("get_lobby")                GET_LOBBY,
        @SerializedName("create_lobby")             CREATE_LOBBY,
        @SerializedName("configure_lobby")          CONFIGURE_LOBBY,
        @SerializedName("join_lobby")               JOIN_LOBBY,
        @SerializedName("leave_lobby")              LEAVE_LOBBY,
        @SerializedName("remove_lobby")             REMOVE_LOBBY,
        @SerializedName("start_game")               START_GAME,
        @SerializedName("end_game")                 END_GAME,
        @SerializedName("reconnect_to_game")        RECONNECT_TO_GAME,
        @SerializedName("pause_game")               PAUSE_GAME,
        @SerializedName("resume_game")              RESUME_GAME,
        @SerializedName("toggle_ready")             TOGGLE_READY,
        @SerializedName("error")                    ERROR,
        @SerializedName("heartbeat")                HEARTBEAT,
        @SerializedName("create_session")           CREATE_SESSION,
        @SerializedName("remove_session")           REMOVE_SESSION,
        @SerializedName("get_sessions")             GET_SESSIONS,
        @SerializedName("change_sessions_order")    CHANGE_SESSIONS_ORDER,
        @SerializedName("start_experiment")         START_EXPERIMENT,
        @SerializedName("end_experiment")           END_EXPERIMENT,
    }

    companion object {
        fun builder(type: Type) = GameRequestBuilder(type)

        val ping = builder(Type.PING).build().toJson()
        val pong = builder(Type.PONG).build().toJson()
        val heartbeat = builder(Type.HEARTBEAT).build().toJson()
    }
}
