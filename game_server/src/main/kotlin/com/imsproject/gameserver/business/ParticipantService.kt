package com.imsproject.gameserver.business

import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.dataAccess.implementations.ParticipantPK
import com.imsproject.gameserver.dataAccess.models.ParticipantDTO
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class ParticipantService(val daoController: DAOController) {

    operator fun contains(userId: Int): Boolean {
        val pk = ParticipantPK(userId)
        return daoController.handleExists(pk)
    }

    fun getAll(): List<ParticipantDTO>{
        return daoController.handleSelectAllParticipants()
    }

    fun remove(userId: Int) {
        val pk = ParticipantDTO(pid = userId)
        daoController.handleDelete(pk)
    }

    fun addParticipant(participant: ParticipantDTO): Int {
        return daoController.handleInsert(participant)
    }
}