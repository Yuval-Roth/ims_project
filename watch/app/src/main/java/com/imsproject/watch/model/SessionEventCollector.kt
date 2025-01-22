package com.imsproject.watch.model

import com.imsproject.common.gameserver.SessionEvent

interface SessionEventCollector {

    /**
     * Collects a new session event.
     *
     * @param event The session event to collect.
     */
    fun addEvent(event: SessionEvent)

    /**
     * Retrieves all collected session events.
     *
     * @return A list of all collected session events.
     */
    fun getAllEvents(): Collection<SessionEvent>


    /**
     * Clears all collected session events.
     */
    fun clearEvents()
}
