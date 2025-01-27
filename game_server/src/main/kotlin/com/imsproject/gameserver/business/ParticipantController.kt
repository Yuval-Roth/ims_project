package com.imsproject.gameserver.business

import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.dataAccess.SectionEnum
import com.imsproject.gameserver.dataAccess.implementations.ParticipantPK
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ParticipantController(val daoController: DAOController) {

    operator fun contains(userId: Int): Boolean {
        val pk = ParticipantPK(userId)
        return daoController.handleExists(SectionEnum.PARTICIPANT,pk)
    }

    fun getAll(): List<Participant>{
        return daoController.handleSelectAll(SectionEnum.PARTICIPANT)
    }

    fun remove(userId: Int) {
        val pk = ParticipantPK(userId)
        daoController.handleDelete(SectionEnum.PARTICIPANT,pk)
    }

    fun addParticipant(participant: Participant): Int {
        return daoController.handleInsert(SectionEnum.PARTICIPANT,participant)
    }
}