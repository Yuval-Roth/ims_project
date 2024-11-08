package com.imsproject.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Response {

    private final String message;
    private final boolean success;
    private final List<String> payload;

    /**
     * Each object in the payload list parameter will be serialized using {@link JsonUtils#serialize(Object)}
     */
    public <T> Response(String message, boolean success, List<T> payload) {
        this.message = message;
        this.success = success;
        this.payload = payload == null ? null : payload.isEmpty() ? null : payload.stream()
                .map(obj -> obj instanceof String str ? str : JsonUtils.serialize(obj))
                .toList();

    }

    public Response(String message, boolean success, String payload) {
        this.message = message;
        this.success = success;
        this.payload = payload == null ? null : List.of(payload);
    }

    /**
     * @param success If the request was successful or not
     */
    public Response(boolean success) {
        this.message = null;
        this.success = success;
        this.payload = null;
    }

    public String message() {
        return message;
    }

    public boolean success() {
        return success;
    }

    /**
     * @param typeOfT Type of the object for deserialization
     * @return Deserialized object of type T
     * <br/><br/><br/>examples for a type definition:<br/><br/><code>1) Type type = new TypeToken&lt;LinkedList&lt;SomeClass&gt;&gt;(){}.getType();</code>
     * <br/><br/><code>2) Type type = SomeClass.class;</code>
     */
    public <T> List<T> payload(Type typeOfT) {
        if(payload == null) {
            return null;
        }
        List<T> list = new ArrayList<>();
        payload.forEach(p -> list.add(JsonUtils.deserialize(p, typeOfT)));
        return list;
    }

    public List<String> payload() {
        return payload;
    }

    /**
     * This method will serialize the response object using {@link JsonUtils#serialize(Object)}
     */
    public String toJson(){
        return JsonUtils.serialize(this);
    }

    /**
     * This method will deserialize the json string using {@link JsonUtils#deserialize(String, Type)}
     */
    public static Response fromJson(String json){
        return JsonUtils.deserialize(json, Response.class);
    }

    /**
     * Equivalent to {@code new Response("",true,List.of()).toJson()}
     */
    public static <T> String getOk(){
        return getOk(List.of());
    }

    /**
     * Equivalent to {@code new Response("",true,List.of(payload)).toJson()}
     */
    public static <T> String getOk(T payload) {
        return getOk(payload == null ? null : List.of(payload));
    }

    /**
     * Equivalent to {@code new Response("",true,payload).toJson()}
     */
    public static <T> String getOk(List<T> payload) {
        return new Response(null,true, payload).toJson();
    }

    /**
     * This method will return a response object with the message as the exception message and success as false.
     * @apiNote if the exception has a cause, the cause message will be added to the response object in the data field as a string.
     * Otherwise, the data field will be an empty string
     */
    public static String getError(Exception e){

        String cause = "";
        if(e.getCause() != null){
            cause = e.getCause().getMessage();
        }
        return new Response(e.getMessage(), false, cause).toJson();
    }

    /**
     * Equivalent to {@code new Response(error,false,List.of()).toJson()}
     */
    public static String getError(String error) {
        return new Response(error,false, List.of()).toJson();
    }
}
