    package com.imsproject.common.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

class CoroutineMonitor {
    private val lock = Mutex(false)
    private val jobs = mutableListOf<Job>()

    suspend fun wait(){
        wait(Long.MAX_VALUE)
    }

    suspend fun wait(time:Long){
        val job : Job
        lock.withLock {
            with(CoroutineScope(coroutineContext)){
                job = launch {
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
                jobs.add(job)
            }
        }
        job.join()
    }

    suspend fun notify(){
        lock.withLock {
            jobs.removeFirstOrNull()?.cancel()
        }
    }

    suspend fun notifyAll(){
        lock.withLock {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }
}