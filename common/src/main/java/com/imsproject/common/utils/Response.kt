package com.imsproject.common.utils

import com.imsproject.common.utils.JsonUtils.deserialize
import com.imsproject.common.utils.JsonUtils.serialize
import java.lang.reflect.Type

class Response {
    private val message: String?
    private val success: Boolean
    private val payload: List<String>?

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

    fun message(): String? {
        return message
    }

    fun success(): Boolean {
        return success
    }

    /**
     * examples for a type definition:
     * 1. `Type type = new TypeToken<LinkedList<SomeClass>>(){}.getType();`
     * 2. `Type type = SomeClass.class;`
     *
     * @param typeOfT Type of the object for deserialization
     * @return Deserialized object of type T. If the payload is null, an empty list will be returned
     */
    fun <T> payload(typeOfT: Type): List<T>? {
        if (payload == null) {
            return null
        }
        return payload.map{ deserialize(it, typeOfT) }
    }

    fun payload(): List<String>? {
        return payload
    }

    /**
     * This method will serialize the response object using [JsonUtils.serialize]
     */
    fun toJson(): String {
        return serialize(this)
    }

    companion object {
        /**
         * This method will deserialize the json string using [JsonUtils.deserialize]
         */
        fun fromJson(json: String): Response {
            return deserialize(json, Response::class.java)
        }

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
