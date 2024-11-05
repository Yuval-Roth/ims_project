package com.imsproject.utils;

public class WebRTCRequestBuilder {

    private WebRTCRequest.Type type;
    private String from;
    private String to;
    private String data;
    private WebRTCRequest.Candidate candidate;

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

    public WebRTCRequestBuilder setCandidate(WebRTCRequest.Candidate candidate) {
        this.candidate = candidate;
        return this;
    }

    public WebRTCRequest build() {
        return new WebRTCRequest(type, from, to, data, candidate);
    }
}
