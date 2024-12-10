package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName
import com.imsproject.common.utils.JsonUtils

data class GameAction(
    val type : Type,
    val actor : String?,
    val data : String?,
    val inSync : Boolean?
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
        return JsonUtils.serialize(this)

        //TODO: in the future, we need to write a custom serializer
        // because json serialization is not efficient
//        return type.name +
//            (actor?.let {"\n$it"} ?: "") +
//            (data?.let {"\n$it"} ?: "") +
//            (inSync?.let {"\n$it"} ?: "")
    }

    companion object {

        private val pingString = builder(Type.PING).build().toString()
        private val pongString = builder(Type.PONG).build().toString()

        private val ping = builder(Type.PING).build()
        private val pong = builder(Type.PONG).build()

        fun fromString(message: String): GameAction {
            return JsonUtils.deserialize(message, GameAction::class.java)

            //TODO: in the future, we need to write a custom deserializer
            // because json serialization is not efficient
//            val lines = message.split('\n')
//            return when(lines[0]){
//                "PING" -> ping
//                "PONG" -> pong
//                "CLICK" -> {
//                    if(lines.size != 3) throw IllegalArgumentException("Invalid CLICK message: $message")
//                    builder(Type.CLICK)
//                        .actor(lines[1])
//                        .inSync(lines[2] == "true")
//                        .build()
//                }
//                "POSITION" -> {
//                    if(lines.size != 4) throw IllegalArgumentException("Invalid POSITION message: $message")
//                    builder(Type.POSITION)
//                        .actor(lines[1])
//                        .data(lines[2])
//                        .inSync(lines[3] == "true")
//                        .build()
//                }
//                "ENTER" -> {
//                    val data = if(lines.size > 1) lines[1] else null
//                    builder(Type.ENTER).data(data).build()
//                }
//                else -> throw IllegalArgumentException("Unknown gameAction type: ${lines[0]}")
//            }
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
    }

}

