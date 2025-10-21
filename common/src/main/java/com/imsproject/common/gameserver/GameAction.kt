package com.imsproject.common.gameserver

data class GameAction (
    val type : Type,
    val actor : String?,
    val data : String?,
    val timestamp : String?,
    val sequenceNumber : Long?
) {
    enum class Type {
        ENTER,
        PING,
        PONG,
        USER_INPUT,
        HEARTBEAT,
        SYNC
    }

    override fun toString(): String {
        return "$type;${actor ?: ""};${data ?: ""};${timestamp ?: ""};${sequenceNumber ?: ""}"
    }

    companion object {

        val ping = builder(Type.PING).build().toString()
        val pong = builder(Type.PONG).build().toString()
        val heartbeat = builder(Type.HEARTBEAT).build().toString()

        fun fromString(message: String): GameAction {
            val parts = message.split(";")
            if(parts.size != 5){
                throw IllegalArgumentException("GameAction string must have 5 parts, but found ${parts.size}")
            }
            return GameAction(
                Type.valueOf(parts[0]),
                if (parts[1] == "") null else parts[1],
                if (parts[2] == "") null else parts[2],
                if (parts[3] == "") null else parts[3],
                if (parts[4] == "") null else parts[4].toLong()
            )
        }

        fun builder(type: Type): GameActionBuilder {
            return GameActionBuilder(type)
        }
    }

}

