package com.imsproject.common.networking

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.jvm.optionals.getOrElse

class PingTracker {

    // this will be called periodically to update the current ping
    var onUpdate : (Long) -> Unit = {}

    private val values = ConcurrentLinkedDeque<Long>()
    private var job : Job? = null
    private var sentCount = 0
    private var receivedCount = 0

    fun lostCount() = sentCount - receivedCount

    fun add(value: Long) {
        values.addLast(value)
        receivedCount++
    }

    fun addSent(){
        sentCount++
    }

    fun start(){
        if(job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            while(true){
                val avg : Long = values.stream()
                    .reduce { prev, value -> prev + value }
                    .map { num -> num / values.size }
                    .getOrElse { -1L }
                values.clear()
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