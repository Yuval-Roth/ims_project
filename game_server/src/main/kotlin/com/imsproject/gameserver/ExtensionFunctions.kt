package com.imsproject.gameserver

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.SocketAddress
import java.time.LocalDateTime

const val HEARTBEAT_TIMEOUT_THRESHOLD = 30L

fun WebSocketSession.send(message: String) {
    this.sendMessage(TextMessage(message))
}

fun WebSocketSession.remoteAddress(): String {
    return this.remoteAddress.toString().substring(1).split(":")[0]
}

fun SocketAddress.toHostPortString() : String {
    val (host,port) = this.toHostPort()
    return "$host:$port"
}

fun SocketAddress.toHostPort() : Pair<String,Int> {
    val cleanAddress = this.toString().substring(1)
    val parts = cleanAddress.split(":")
    return Pair(parts[0], parts[1].toInt())
}

fun String.toResponseEntity (errorCode: HttpStatusCode): ResponseEntity<String> {
    return ResponseEntity.status(errorCode).body(this)
}

fun String.toResponseEntity (): ResponseEntity<String> {
    return ResponseEntity.ok(this)
}

fun LocalDateTime.isMoreThanSecondsAgo(seconds: Long): Boolean {
    return this.isAfter(LocalDateTime.now().minusSeconds(seconds))
}