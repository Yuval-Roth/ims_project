package com.imsproject.gameServer

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

fun WebSocketSession.send(message: String) {
    this.sendMessage(TextMessage(message))
}

fun WebSocketSession.remoteAddress(): String {
    return this.remoteAddress.toString().substring(1).split(":")[0]
}