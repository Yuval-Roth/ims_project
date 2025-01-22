package com.imsproject.gameserver.business

import com.google.gson.JsonParseException
import com.imsproject.common.etc.TimeRequest
import com.imsproject.common.networking.UdpClient
import com.imsproject.common.utils.fromJson
import com.imsproject.common.utils.toJson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.SocketTimeoutException

@Component
class TimeServerHandler {

    @Value("\${running.local}")
    private var runningLocal: Boolean = false

    @Value("\${time_server.port}")
    private var timeServerPort: Int = 0

    private lateinit var timeServerIp: String

    final var timeServerDelta: Long = 0
        private set

    private val timeServerUdp = UdpClient()

    /**
     * Sends a request to the time server to get the current time in milliseconds.
     * This method will retry up to 3 times in case of a timeout.
     *
     * @return The current time in milliseconds as reported by the time server.
     * @throws SocketTimeoutException If the request times out after 3 attempts.
     * @throws JsonParseException If the response from the time server cannot be parsed.
     * @throws IOException If there is an I/O error while sending or receiving the request.
     */
    fun timeServerCurrentTimeMillis(): Long {
        val request = TimeRequest.request(TimeRequest.Type.CURRENT_TIME_MILLIS).toJson()
        try{
            val startTime = System.currentTimeMillis()
            timeServerUdp.send(request)
            val response = timeServerUdp.receive()
            val timeDelta = System.currentTimeMillis() - startTime
            val timeResponse = fromJson<TimeRequest>(response)
            val halfRoundTripTime = timeDelta / 2
            return timeResponse.time!! - halfRoundTripTime // approximation
        } catch(e: SocketTimeoutException){
            log.error("Time request timeout", e)
            throw e
        } catch(e: JsonParseException){
            log.error("Failed to parse time response", e)
            throw e
        } catch (e: IOException){
            log.error("Failed to fetch time", e)
            throw e
        }
    }

    private fun run(){
        while(true){
            try{
                val data : List<Long> = List(100) {
                    val currentLocal = System.currentTimeMillis()
                    val currentTimeServer = timeServerCurrentTimeMillis()
                    currentLocal-currentTimeServer
                }
                timeServerDelta = data.average().toLong()
            } catch(e: Exception){
                log.error("Failed to fetch time from time server", e)
                continue
            }
            Thread.sleep(10000)
        }
    }

    @EventListener
    fun onApplicationReady(event: ApplicationReadyEvent){
        timeServerIp = if(runningLocal) "localhost" else "host.docker.internal"
        // set up udp client for time server
        timeServerUdp.remoteAddress = timeServerIp
        timeServerUdp.remotePort = timeServerPort
        timeServerUdp.init()
        timeServerUdp.setTimeout(2000)
        Thread(this::run).start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(TimeServerHandler::class.java)
    }
}