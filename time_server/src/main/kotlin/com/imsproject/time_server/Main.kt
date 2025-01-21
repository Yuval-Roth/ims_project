package com.imsproject.time_server

import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson
import java.time.LocalDateTime

fun main() {

    // create a UDP client
    val udpClient = UdpClient()
    udpClient.localPort = 8642
    udpClient.init()
    println("${LocalDateTime.now().prettyPrint()} - Time server started on port ${udpClient.localPort}")

    // handle incoming requests
    while(true){
        val packet = udpClient.receiveRaw()
        val request: TimeRequest
        val json = String(packet.data, 0, packet.length)
        try{
            request = fromJson(json)
        } catch(e: Exception){
            System.err.println("${LocalDateTime.now().prettyPrint()} - Error parsing message: $json\n${e.stackTraceToString()}")
            continue
        }
        when(request.type){
            TimeRequest.Type.CURRENT_TIME_MILLIS -> udpClient.send(
                TimeRequest.currentTimeMillis().toJson(),
                packet.address.hostAddress,
                packet.port
            )
            TimeRequest.Type.NANO_TIME -> udpClient.send(
                TimeRequest.nanoTime().toJson(),
                packet.address.hostAddress,
                packet.port
            )
            else -> {}
        }
    }
}

fun LocalDateTime.prettyPrint(): String {
    return StringBuilder()
        .append(this.year)
        .append("-")
        .append(this.monthValue.toString().padStart(2, '0'))
        .append("-")
        .append(this.dayOfMonth.toString().padStart(2, '0'))
        .append(" ")
        .append(this.hour.toString().padStart(2, '0'))
        .append(":")
        .append(this.minute.toString().padStart(2, '0'))
        .append(":")
        .append(this.second.toString().padStart(2, '0'))
        .toString()
}