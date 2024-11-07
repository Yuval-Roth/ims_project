package com.imsproject.utils.webrtc;

public class WebRTCRequestBuilder {

    private WebRTCRequest.Type type;
    private String from;
    private String to;
    private String data;

    public WebRTCRequestBuilder setType(WebRTCRequest.Type type) {
        this.type = type;
        return this;
    }

    public WebRTCRequestBuilder setFrom(String from) {
        this.from = from;
        return this;
    }

    public WebRTCRequestBuilder setTo(String to) {
        this.to = to;
        return this;
    }

    public WebRTCRequestBuilder setData(String data) {
        this.data = data;
        return this;
    }

    public WebRTCRequest build() {
        return new WebRTCRequest(type, from, to, data);
    }
}
