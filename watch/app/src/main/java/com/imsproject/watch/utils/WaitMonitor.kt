package com.imsproject.watch.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class WaitMonitor(
    private val scope: CoroutineScope,
    private val supportPreemptiveWakeUp: Boolean = false
) {

    private var job: Job? = null
    private var wokenUp = false

    suspend fun wait(){
        if(supportPreemptiveWakeUp && wokenUp){
            wokenUp = false
            return
        }

        job = scope.launch {
            try {
                delay(Long.MAX_VALUE)
            } catch (e: CancellationException){
                // do nothing
            }
        }
        job?.join()
        wokenUp = false
        job = null
    }

    suspend fun wait(time:Long){
        if(supportPreemptiveWakeUp && wokenUp){
            wokenUp = false
            return
        }

        job = scope.launch {
            try {
                withTimeout(time) {
                    delay(time)
                }
            } catch (e: TimeoutCancellationException){
                // do nothing
            } catch (e: CancellationException){
                // do nothing
            }
        }
        job?.join()
        wokenUp = false
        job = null
    }

    fun wakeup(){
        if(supportPreemptiveWakeUp) {
            wokenUp = true
        }
        job?.cancel()
    }
}