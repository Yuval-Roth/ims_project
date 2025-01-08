package com.imsproject.watch.model

import android.se.omapi.Session
import com.imsproject.common.gameserver.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import java.util.TreeSet

class SessionEventCollectorImpl private constructor() : SessionEventCollector {

    private val events = TreeSet<SessionEvent>()
    private val executor = Executors.newSingleThreadExecutor()

    override fun addEvent(event: SessionEvent) {
        executor.submit {
            events.add(event)
        }
    }

    override fun getAllEvents(): Iterable<SessionEvent> {
        return events
    }

    override fun clearEvents() {
        executor.submit {
            events.clear()
        }
    }

    companion object {
        private val lock = Any()
        private var instance: SessionEventCollectorImpl? = null

        fun getInstance(): SessionEventCollectorImpl {
            synchronized(lock) {
                if (instance == null) {
                    instance = SessionEventCollectorImpl()
                }
                return instance!!
            }
        }
    }
}