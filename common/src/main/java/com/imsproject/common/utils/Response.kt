package com.imsproject.common.utils

import com.imsproject.common.utils.JsonUtils.serialize

class Response {
    val message: String?
    val success: Boolean
    /**
     * raw payload list, contains either actual strings or serialized objects
     */
    val payload: List<String>?

    /**
     * Each object in the payload list parameter will be serialized using [JsonUtils.serialize]
     */
    constructor(message: String?, success: Boolean, payload: List<Any>?) {
        this.message = message
        this.success = success
        this.payload = payload?.map{
            if (it is String) it
            else serialize(it)
        }
    }

    constructor(message: String?, success: Boolean, payload: String) : this(message, success, listOf(payload))

    /**
     * @param success If the request was successful or not
     */
    constructor(success: Boolean) : this(null, success, null)


    /**
     * This method will deserialize the payload list to the specified type and return it as a typed list.
     */
    inline fun <reified T> typedPayload(): List<T>? {
        if (payload == null) {
            return null
        } else if (T::class == String::class){
            @Suppress("UNCHECKED_CAST")
            return payload as List<T>
        }
        return payload.map{ fromJson(it) }
    }

    companion object {

        fun getOk(): String {
            return Response(true).toJson()
        }

        /**
         * Equivalent to `new Response("",true,List.of(payload)).toJson()`
         */
        fun getOk(payload: Any): String {
            return getOk(listOf(payload))
        }

        /**
         * Equivalent to `new Response("",true,payload).toJson()`
         */
        fun getOk(payload: List<Any>): String {
            return Response(null, true, payload).toJson()
        }

        /**
         * This method will return a response object with the message as the exception message and success as false.
         * @apiNote if the exception has a cause, the cause message will be added to the response object in the data field as a string.
         * Otherwise, the data field will be an empty string
         */
        fun getError(e: Exception): String {
            val cause = e.cause?.message ?: ""
            return Response(e.message, false, cause).toJson()
        }

        /**
         * Equivalent to `new Response(error,false,List.of()).toJson()`
         */
        fun getError(error: String): String {
            return Response(error, false, null).toJson()
        }
    }
}
