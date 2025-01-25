package com.imsproject.gameserver.business

import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.dataAccess.DAOController
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ParticipantController(val daoController: DAOController) {

    //todo: sionara fields
    val participants = ConcurrentHashMap<String, Participant>()
    val idGenerator = SimpleIdGenerator(3)

    operator fun contains(userId: String): Boolean {
        return participants.containsKey(userId)
    }

    fun getAll(): List<Participant>{
        return participants.values.toList()
    }

    fun remove(userId: String) : Boolean {
        return participants.remove(userId) != null
    }

    fun addParticipant(participant: Participant): String {
        val id = idGenerator.generate()
        participants[id] = participant.copy(pid = id)
        // todo: add bdikat kelet
        return id
    }
}