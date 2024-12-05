package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

data class GameRequest(
    val type: Type,
    val playerId: String?,
    val lobbyId : String?,
    val gameType : GameType?,
    val success : Boolean?,
    val message : String?,
    val data : List<String>?,

    //TODO: Add more fields if needed
){
    enum class Type(val value: String) {
        @SerializedName("ping") PING("ping"),
        @SerializedName("pong") PONG("pong"),
        @SerializedName("enter") ENTER("enter"),
        @SerializedName("exit") EXIT("exit"),
        @SerializedName("get_online_player_ids") GET_ONLINE_PLAYER_IDS("get_online_player_ids"),
        @SerializedName("get_lobbies") GET_ALL_LOBBIES("get_lobbies"),
        @SerializedName("get_lobby") GET_LOBBY("get_lobby"),
        @SerializedName("create_lobby") CREATE_LOBBY("create_lobby"),
        @SerializedName("set_lobby_type") SET_LOBBY_TYPE("set_lobby_type"),
        @SerializedName("join_lobby") JOIN_LOBBY("join_lobby"),
        @SerializedName("leave_lobby") LEAVE_LOBBY("leave_lobby"),
        @SerializedName("start_game") START_GAME("start_game"),
        @SerializedName("end_game") END_GAME("end_game"),
        @SerializedName("pause_game") PAUSE_GAME("pause_game"),
        @SerializedName("resume_game") RESUME_GAME("resume_game"),
        @SerializedName("toggle_ready") TOGGLE_READY("toggle_ready"),
        @SerializedName("error") ERROR("error");
    }

    fun toJson() = JsonUtils.serialize(this)

    companion object {
        fun builder(type: Type) = GameRequestBuilder(type)

        fun pong() = builder(Type.PONG).build().toJson()
        fun ping() = builder(Type.PING).build().toJson()

        fun fromJson(json: String) = JsonUtils.deserialize<GameRequest>(json, GameRequest::class.java)
    }
}
