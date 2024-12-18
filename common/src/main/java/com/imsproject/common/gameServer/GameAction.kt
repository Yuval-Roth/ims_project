package com.imsproject.common.gameServer

data class GameAction(
    val type : Type,
    val actor : String?,
    val data : String?,
    val inSync : Boolean?
    //TODO: Add more fields if needed
) {
    enum class Type {
        ENTER,
        PING,
        PONG,
        POSITION,
        CLICK,
        HEARTBEAT
    }

    override fun toString(): String {
        return "$type;${actor ?: ""};${data ?: ""};${inSync ?: ""}"
    }

    companion object {

        val ping = builder(Type.PING).build().toString()
        val pong = builder(Type.PONG).build().toString()
        val heartbeat = builder(Type.HEARTBEAT).build().toString()

        fun fromString(message: String): GameAction {
            val parts = message.split(";")
            return GameAction(
                Type.valueOf(parts[0]),
                if (parts[1] == "") null else parts[1],
                if (parts[2] == "") null else parts[2],
                if (parts[3] == "") null else parts[3].toBoolean()
            )
        }

        fun builder(type: Type): GameActionBuilder {
            return GameActionBuilder(type)
        }
    }

}

