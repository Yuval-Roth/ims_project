package com.imsproject.utils;

public record WebRTCRequest(Type type, String to, String data) {
    public enum Type{
        OFFER,
        ANSWER,
        CANDIDATE
    }

    public static WebRTCRequest fromJson(String json) {
        return JsonUtils.deserialize(json, WebRTCRequest.class);
    }

    public String toJson() {
        return JsonUtils.serialize(this);
    }
}
