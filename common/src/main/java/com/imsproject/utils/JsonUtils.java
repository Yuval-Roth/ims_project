package com.imsproject.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class JsonUtils {

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>{
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    private static class LocalTimeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime>{
        @Override
        public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalTime.parse(json.getAsString());
        }
    }

    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>{
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString());
        }
    }

    private static final Gson gson =  new GsonBuilder()
//            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class,new LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate.class,new LocalDateAdapter())
            .registerTypeAdapter(LocalTime.class,new LocalTimeAdapter())
            .enableComplexMapKeySerialization()
            .create();

    public static <T> String serialize(T obj) {
        if(obj == null) return null;
        if(obj instanceof String str) return str;
        return gson.toJson(obj);
    }

    public static <T> T deserialize(String json, Type TypeOfT) {
        return gson.fromJson(json, TypeOfT);
    }
}

