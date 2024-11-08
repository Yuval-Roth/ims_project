package com.imsproject.utils.webrtc;

import com.google.gson.annotations.SerializedName;
import com.imsproject.utils.JsonUtils;

public record WebRTCRequest(Type type, String from, String to, String data) {

    public enum Type {
        @SerializedName("enter")
        ENTER("enter"),
        @SerializedName("exit")
        EXIT("exit"),
        @SerializedName("offer")
        OFFER("offer"),
        @SerializedName("answer")
        ANSWER("answer"),
        @SerializedName("ice_candidates")
        ICE_CANDIDATES("ice_candidates");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

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
