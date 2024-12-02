package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName

data class GameAction(
    val type : Type,
    val data : String?
    //TODO: Add more fields if needed
) {
    enum class Type(private val value: String) {
        @SerializedName("enter")
        ENTER("enter"),
        @SerializedName("ping")
        PING("ping"),
        @SerializedName("pong")
        PONG("pong"),
        @SerializedName("position")
        POSITION("position"),
        @SerializedName("click")
        CLICK("click");

        override fun toString(): String{
            return this.value
        }
    }

    override fun toString(): String {
        return type.name + (data?.let {"\n$it"} ?: "")
    }

    companion object {

        private val pingString = builder(Type.PING).build().toString()
        private val pongString = builder(Type.PONG).build().toString()
        private val clickString = builder(Type.CLICK).build().toString()
        private val posBuilder = builder(Type.POSITION)

        private val ping = builder(Type.PING).build()
        private val pong = builder(Type.PONG).build()
        private val click = builder(Type.CLICK).build()

        fun fromString(message: String): GameAction {
            val lines = message.split('\n')
            return when(lines[0]){
                "PING" -> ping
                "PONG" -> pong
                "CLICK" -> click
                "POSITION" -> {
                    if(lines.size != 2) throw IllegalArgumentException("Invalid POSITION message: $message")
                    val ret = posBuilder.data(lines[1]).build()
                    posBuilder.data(null)
                    ret
                }
                "ENTER" -> {
                    val data = if(lines.size > 1) lines[1] else null
                    builder(Type.ENTER).data(data).build()
                }
                else -> throw IllegalArgumentException("Unknown gameAction type: ${lines[0]}")
            }
        }

        fun builder(type: Type): GameActionBuilder {
            return GameActionBuilder(type)
        }

        /**
         * returns a cached value for efficiency
         */
        fun ping() = pingString

        /**
         * returns a cached value for efficiency
         */
        fun pong() = pongString

        /**
         * returns a cached value for efficiency
         */
        fun click() = clickString
    }

}

