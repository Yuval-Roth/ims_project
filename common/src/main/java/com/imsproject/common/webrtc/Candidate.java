package com.imsproject.common.webrtc;

public record Candidate(String sdp, String sdpMid, int sdpMLineIndex) {
}
