package com.imsproject.utils;

public record WebRTCRequest(Type type, String from, String to, String data) {

    public enum Type {
        ENTER("enter"),
        EXIT("exit"),
        OFFER("offer"),
        ANSWER("answer"),
        ICE_CANDIDATES("ice_candidates"),
        USER_FOUND("user_found"),
        USER_NOT_FOUND("user_not_found"),
        ;

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    public record Candidate (String candidate, String sdpMid, int sdpMLineIndex) { }

    public static WebRTCRequest fromJson(String json) {
        return JsonUtils.deserialize(json, WebRTCRequest.class);
    }

    public String toJson() {
        return JsonUtils.serialize(this);
    }

    public static WebRTCRequestBuilder builder() {
        return new WebRTCRequestBuilder();
    }
}
