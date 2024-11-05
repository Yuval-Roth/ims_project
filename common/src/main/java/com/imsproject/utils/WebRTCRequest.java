package com.imsproject.utils;

public record WebRTCRequest(Type type, String from, String to, String data, Candidate candidate) {

    public enum Type {
        OFFER("offer"),
        ANSWER("answer"),
        CANDIDATE("candidate");

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
