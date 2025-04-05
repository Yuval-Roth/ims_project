package com.imsproject.gameserver.business

import com.imsproject.common.utils.SimpleIdGenerator
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.dataAccess.SectionEnum
import com.imsproject.gameserver.dataAccess.implementations.ParticipantPK
import com.imsproject.gameserver.dataAccess.models.ParticipantDTO
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ParticipantController(val daoController: DAOController) {

    operator fun contains(userId: Int): Boolean {
        val pk = ParticipantPK(userId)
        return daoController.handleExists(SectionEnum.PARTICIPANT,pk)
    }

    fun getAll(): List<ParticipantDTO>{
        return daoController.handleSelectAll(SectionEnum.PARTICIPANT)
    }

    fun remove(userId: Int) {
        val pk = ParticipantDTO(pid = userId)
        daoController.handleDelete(SectionEnum.PARTICIPANT,pk)
    }

    fun addParticipant(participant: ParticipantDTO): Int {
        return daoController.handleInsert(participant)
    }
}