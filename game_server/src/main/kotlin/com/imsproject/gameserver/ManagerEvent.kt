package com.imsproject.gameserver

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

class ManagerEvent internal constructor (
    val type: Type,
    val playerId: String? = null,
    val lobbyId: String? = null,
) {
    enum class Type {
        @SerializedName("register_for_events") REGISTER_FOR_EVENTS,
        @SerializedName("ping") PING,
        @SerializedName("pong") PONG,
        @SerializedName("heartbeat") HEARTBEAT,
        @SerializedName("player_connected") PLAYER_CONNECTED,
        @SerializedName("player_disconnected") PLAYER_DISCONNECTED,
        @SerializedName("player_ready_toggle") PLAYER_READY_TOGGLE,
        @SerializedName("game_ended") GAME_ENDED,
    }

    fun toJson(): String{
        return JsonUtils.serialize(this)
    }

    companion object {
        fun fromJson(json: String): ManagerEvent {
            return JsonUtils.deserialize(json)
        }

        fun builder(type: Type): ManagerEventBuilder {
            return ManagerEventBuilder(type)
        }

        val ping = builder(Type.PING).build().toString()
        val pong = builder(Type.PONG).build().toString()
        val heartbeat = builder(Type.HEARTBEAT).build().toString()
    }
}