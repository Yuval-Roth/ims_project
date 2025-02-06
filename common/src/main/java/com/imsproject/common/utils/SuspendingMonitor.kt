    package com.imsproject.common.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

class SuspendingMonitor {
    private val lock = Mutex(false)
    private val jobs = mutableListOf<Job>()

    suspend fun wait(){
        val job : Job
        lock.withLock {
            with(CoroutineScope(coroutineContext)){
                job = launch {
                    try {
                        awaitCancellation()
                    } catch (e: CancellationException){
                        // do nothing
                    }
                }
                jobs.add(job)
            }
        }
        job.join()
    }

    suspend fun wait(time:Long){
        val job : Job
        lock.withLock {
            with(CoroutineScope(coroutineContext)){
                job = launch {
                    try {
                        withTimeoutOrNull<Nothing>(time) {
                            awaitCancellation()
                        }
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