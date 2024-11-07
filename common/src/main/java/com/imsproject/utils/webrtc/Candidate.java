package com.imsproject.utils.webrtc;

public record Candidate(String candidate, String sdpMid, int sdpMLineIndex) {
}
