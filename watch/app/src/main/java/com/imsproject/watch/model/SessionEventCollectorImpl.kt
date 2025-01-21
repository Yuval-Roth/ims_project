package com.imsproject.watch.model

import com.imsproject.common.gameserver.SessionEvent
import java.util.concurrent.ConcurrentLinkedDeque

class SessionEventCollectorImpl private constructor() : SessionEventCollector {

    private val events = ConcurrentLinkedDeque<SessionEvent>()

    override fun addEvent(event: SessionEvent) {
        events.add(event)
    }

    override fun getAllEvents(): Collection<SessionEvent> {
        return events
    }

    override fun clearEvents() {
        events.clear()
    }

    companion object {
        private val lock = Any()
        private var instance: SessionEventCollector? = null

        fun getInstance(): SessionEventCollector {
            synchronized(lock) {
                if (instance == null) {
                    instance = SessionEventCollectorImpl()
                }
                return instance!!
            }
        }
    }
}