package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

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
    val timestamp: String?
){
    enum class Type {
        @SerializedName("ping")                     PING,
        @SerializedName("pong")                     PONG,
        @SerializedName("enter")                    ENTER,
        @SerializedName("exit")                     EXIT,
        @SerializedName("get_online_player_ids")    GET_ONLINE_PLAYER_IDS,
        @SerializedName("get_lobbies")              GET_ALL_LOBBIES,
        @SerializedName("get_lobby")                GET_LOBBY,
        @SerializedName("create_lobby")             CREATE_LOBBY,
        @SerializedName("set_lobby_type")           SET_LOBBY_TYPE,
        @SerializedName("set_game_duration")        SET_GAME_DURATION,
        @SerializedName("join_lobby")               JOIN_LOBBY,
        @SerializedName("leave_lobby")              LEAVE_LOBBY,
        @SerializedName("remove_lobby")             REMOVE_LOBBY,
        @SerializedName("start_game")               START_GAME,
        @SerializedName("end_game")                 END_GAME,
        @SerializedName("pause_game")               PAUSE_GAME,
        @SerializedName("resume_game")              RESUME_GAME,
        @SerializedName("toggle_ready")             TOGGLE_READY,
        @SerializedName("error")                    ERROR,
        @SerializedName("heartbeat")                HEARTBEAT,
        @SerializedName("create_session")           CREATE_SESSION,
        @SerializedName("remove_session")           REMOVE_SESSION,
        @SerializedName("get_sessions")             GET_SESSIONS,
        @SerializedName("change_sessions_order")    CHANGE_SESSIONS_ORDER

        ;
        
        override fun toString(): String {
            return name.lowercase()
        }
    }

    fun toJson() = JsonUtils.serialize(this)

    companion object {
        fun builder(type: Type) = GameRequestBuilder(type)

        val ping = builder(Type.PING).build().toJson()
        val pong = builder(Type.PONG).build().toJson()
        val heartbeat = builder(Type.HEARTBEAT).build().toJson()

        fun fromJson(json: String) : GameRequest = JsonUtils.deserialize(json)
    }
}
