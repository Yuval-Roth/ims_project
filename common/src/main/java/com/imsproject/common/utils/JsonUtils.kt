package com.imsproject.common.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object JsonUtils {
    private val gson: Gson = GsonBuilder()
        //.setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .enableComplexMapKeySerialization()
        .create()

    fun <T> serialize(obj: T): String {
        if (obj is String) return obj
        return gson.toJson(obj)
    }

    fun <T> deserialize(json: String, typeOfT: Type): T {
        return gson.fromJson(json, typeOfT)
    }

    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
            return LocalDateTime.parse(json.asString)
        }
    }

    private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        override fun serialize(src: LocalTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime {
            return LocalTime.parse(json.asString)
        }
    }

    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
            return LocalDate.parse(json.asString)
        }
    }
}
