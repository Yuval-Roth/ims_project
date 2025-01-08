package com.imsproject.common.networking

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.jvm.optionals.getOrElse

class PingTracker (val scope: CoroutineScope) {

    // this will be called periodically to update the current ping
    var onUpdate : (Long) -> Unit = {}

    private val waitingForAck = ConcurrentLinkedDeque<Long>()
    private val values = ConcurrentLinkedDeque<Long>()
    private var job : Job? = null
    private var sentCount = 0
    private var receivedCount = 0

    fun lostCount() = sentCount - receivedCount

    fun receivedAt(value: Long) {
        waitingForAck.poll()?.let { timestamp ->
            values.add(value - timestamp)
        }
        receivedCount++
    }

    fun sentAt(timestamp: Long){
        waitingForAck.add(timestamp)
        sentCount++
    }

    fun start(){
        if(job != null) return
        job = scope.launch(Dispatchers.IO) {
            while(true){
                val avg : Long = values.stream()
                    .reduce { prev, value -> prev + value }
                    .map { num -> num / values.size }
                    .getOrElse { -1L }
                values.clear()
                waitingForAck.clear()
                onUpdate(avg)
                delay(1000)
            }
        }
    }

    fun stop(){
        job?.cancel()
        job = null
    }
}