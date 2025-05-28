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
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.Semaphore

@Service
class TimeServerService {

    @Value("\${running.local}")
    private var runningLocal: Boolean = false

    @Value("\${time_server.port}")
    private var timeServerPort: Int = 0

    private lateinit var timeServerIp: String

    final var timeServerDelta: Long = 0
        private set

    private val timeServerUdp = UdpClient()
    private val lock = Semaphore(1,true)

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
        lock.acquire()
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
        } finally {
            lock.release()
        }
    }

    private fun run(){
        val request = TimeRequest.request(TimeRequest.Type.CURRENT_TIME_MILLIS).toJson()
        while(true){
            Thread.sleep(10000)
            try{
                lock.acquire()
                var count = 0
                val data = mutableListOf<Long>()
                while(count < 120){
                    try {
                        val currentLocalTime = System.currentTimeMillis()
                        timeServerUdp.send(request)
                        val response = timeServerUdp.receive()
                        val timeDelta = System.currentTimeMillis() - currentLocalTime
                        val timeResponse = fromJson<TimeRequest>(response)
                        val currentServerTime = timeResponse.time!! - timeDelta / 2 // approximation
                        data.add(currentLocalTime-currentServerTime)
                        count++
                    } catch(e: SocketTimeoutException){
                        log.error("Time request timeout", e)
                    } catch(e: JsonParseException){
                        log.error("Failed to parse time response", e)
                    } catch (e: IOException){
                        log.error("Failed to fetch time", e)
                    }
                }
                // remove the first and last 10 values (outliers)
                data.sort()
                data.subList(0, 10).clear()
                data.subList(data.size - 10, data.size).clear()
                timeServerDelta = data.average().toLong()
            } catch(e: Exception){
                log.error("Failed to fetch time from time server", e)
                continue
            } finally {
                lock.release()
            }
        }
    }

    @EventListener
    fun onApplicationReady(event: ApplicationReadyEvent){
        instance = this
        timeServerIp = if(runningLocal) "localhost" else "host.docker.internal"
        // set up udp client for time server
        timeServerUdp.remoteAddress = timeServerIp
        timeServerUdp.remotePort = timeServerPort
        timeServerUdp.init()
        timeServerUdp.setTimeout(2000)
        Thread(this::run).start()
    }

    companion object {
        lateinit var instance: TimeServerService
        private val log = LoggerFactory.getLogger(TimeServerService::class.java)
    }



}