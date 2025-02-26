package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.DaoException
import com.imsproject.gameserver.dataAccess.implementations.*

import com.imsproject.gameserver.dataAccess.models.*
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component
import java.sql.SQLException

enum class SectionEnum {
    PARTICIPANT, EXPERIMENT, SESSION, SESSION_EVENT
}

@Suppress("UNCHECKED_CAST")
@Component
class DAOController(
    private val participantDAO: ParticipantsDAO,
    private val experimentDAO: ExperimentsDAO,
    private val sessionDAO: SessionsDAO,
    private val sessionEventDAO: SessionEventsDAO
) {

    @Throws(DaoException::class)
    fun handleInsert(section: SectionEnum, dto: Any): Int {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.insert(dto as ParticipantDTO)
            SectionEnum.EXPERIMENT -> experimentDAO.insert(dto as ExperimentDTO)
            SectionEnum.SESSION -> sessionDAO.insert(dto as SessionDTO)
            SectionEnum.SESSION_EVENT -> sessionEventDAO.insert(dto as SessionEventDTO)
        }
    }

    @Throws(DaoException::class)
    fun handleInsertAll(section: SectionEnum, dtos: List<Any>): List<Int> {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.insertAll(dtos as List<ParticipantDTO>)
            SectionEnum.EXPERIMENT -> experimentDAO.insertAll(dtos as List<ExperimentDTO>)
            SectionEnum.SESSION -> sessionDAO.insertAll(dtos as List<SessionDTO>)
            SectionEnum.SESSION_EVENT -> sessionEventDAO.insertAll(dtos as List<SessionEventDTO>)
        }
    }

    @Throws(DaoException::class)
    fun handleBulkInsert(section: SectionEnum, dtos: List<Any>): Int {
        return when (section) {
            SectionEnum.PARTICIPANT -> throw UnsupportedOperationException("Bulk insert not supported for participants")
            SectionEnum.EXPERIMENT -> throw UnsupportedOperationException("Bulk insert not supported for experiments")
            SectionEnum.SESSION -> throw UnsupportedOperationException("Bulk insert not supported for sessions")
            SectionEnum.SESSION_EVENT -> sessionEventDAO.bulkInsert(dtos as List<SessionEventDTO>)
        }
    }

    @Throws(DaoException::class)
    fun handleExists(section: SectionEnum, pk: Any): Boolean {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.exists(pk as ParticipantPK)
            SectionEnum.EXPERIMENT -> experimentDAO.exists(pk as ExperimentPK)
            SectionEnum.SESSION -> sessionDAO.exists(pk as SessionPK)
            SectionEnum.SESSION_EVENT -> sessionEventDAO.exists(pk as SessionEventPK)
        }
    }

    @Throws(DaoException::class)
    fun handleDelete(section: SectionEnum, pk: Any) {
        when (section) {
            SectionEnum.PARTICIPANT -> {
                val participantDTO = pk as ParticipantDTO
                if (participantDTO.pid == null) throw Exception("A participant id was not provided for deletion")
                participantDAO.delete(ParticipantPK(participantDTO.pid))
            }
            SectionEnum.EXPERIMENT -> {
                val exp = pk as ExperimentDTO
                if (exp.expId == null) throw Exception("An experiment id was not provided for deletion")
                experimentDAO.delete(ExperimentPK(exp.expId))
            }
            SectionEnum.SESSION -> {
                val sessionDTO = pk as SessionDTO
                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for deletion")
                sessionDAO.delete(SessionPK(sessionDTO.sessionId))
            }
            SectionEnum.SESSION_EVENT -> {
                val sessionEventDTO = pk as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for deletion")
                sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
            }
        }
    }

    @Throws(DaoException::class)
    fun handleUpdate(section: SectionEnum, dto: Any) {
        when (section) {
            SectionEnum.PARTICIPANT -> {
                val participantDTO = dto as ParticipantDTO
                if (participantDTO.pid == null) throw Exception("A participant id was not provided for update")
                participantDAO.update(participantDTO)
            }
            SectionEnum.EXPERIMENT -> {
                val exp = dto as ExperimentDTO
                if (exp.expId == null) throw Exception("An experiment id was not provided for update")
                experimentDAO.update(exp)
            }
            SectionEnum.SESSION -> {
                val sessionDTO = dto as SessionDTO
                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for update")
                sessionDAO.update(sessionDTO)
            }
            SectionEnum.SESSION_EVENT -> {
                val sessionEventDTO = dto as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for update")
                sessionEventDAO.update(sessionEventDTO)
            }
        }
    }

    @Throws(DaoException::class)
    fun handleSelect(section: SectionEnum, pk: Any): Any {
        return when (section) {
            SectionEnum.PARTICIPANT -> {
                val participantDTO = pk as ParticipantDTO
                if (participantDTO.pid == null) throw Exception("A participant id was not provided for selection")
                participantDAO.select(ParticipantPK(participantDTO.pid))
            }
            SectionEnum.EXPERIMENT -> {
                val exp = pk as ExperimentDTO
                if (exp.expId == null) throw Exception("An experiment id was not provided for selection")
                experimentDAO.select(ExperimentPK(exp.expId))
            }
            SectionEnum.SESSION -> {
                val sessionDTO = pk as SessionDTO
                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for selection")
                sessionDAO.select(SessionPK(sessionDTO.sessionId))
            }
            SectionEnum.SESSION_EVENT -> {
                val sessionEventDTO = pk as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for selection")
                sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
            }
        }
    }

    @Throws(DaoException::class)
    fun <T> handleSelectAll(section: SectionEnum): List<T> {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.selectAll() as List<T>
            SectionEnum.EXPERIMENT -> experimentDAO.selectAll() as List<T>
            SectionEnum.SESSION -> sessionDAO.selectAll() as List<T>
            SectionEnum.SESSION_EVENT -> sessionEventDAO.selectAll() as List<T>
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DAOController::class.java)
    }
}
