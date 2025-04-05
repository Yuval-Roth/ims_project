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

//    @Throws(DaoException::class)
//    fun handleInsert(section: SectionEnum, dto: Any): Int {
//        return when (section) {
//            SectionEnum.PARTICIPANT -> participantDAO.insert(dto as ParticipantDTO)
//            SectionEnum.EXPERIMENT -> experimentDAO.insert(dto as ExperimentDTO)
//            SectionEnum.SESSION -> sessionDAO.insert(dto as SessionDTO)
//            SectionEnum.SESSION_EVENT -> sessionEventDAO.insert(dto as SessionEventDTO)
//        }
//    }

    @Throws(DaoException::class)
    fun handleInsert(dto: ParticipantDTO): Int {
        return participantDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: ExperimentDTO): Int {
        return experimentDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: SessionDTO): Int {
        return sessionDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: SessionEventDTO): Int {
        return sessionEventDAO.insert(dto)
    }

//    @Throws(DaoException::class)
//    fun handleInsertAll(section: SectionEnum, dtos: List<Any>): List<Int> {
//        return when (section) {
//            SectionEnum.PARTICIPANT -> participantDAO.insertAll(dtos as List<ParticipantDTO>)
//            SectionEnum.EXPERIMENT -> experimentDAO.insertAll(dtos as List<ExperimentDTO>)
//            SectionEnum.SESSION -> sessionDAO.insertAll(dtos as List<SessionDTO>)
//            SectionEnum.SESSION_EVENT -> sessionEventDAO.insertAll(dtos as List<SessionEventDTO>)
//        }
//    }

    @Throws(DaoException::class)
    fun handleInsertAllParticipants(dtos: List<ParticipantDTO>): List<Int> =
        participantDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllExperiments(dtos: List<ExperimentDTO>): List<Int> =
        experimentDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllSessions(dtos: List<SessionDTO>): List<Int> =
        sessionDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllSessionEvents(dtos: List<SessionEventDTO>): List<Int> =
        sessionEventDAO.insertAll(dtos)

//
//    @Throws(DaoException::class)
//    fun handleBulkInsert(section: SectionEnum, dtos: List<Any>): Int {
//        return when (section) {
//            SectionEnum.PARTICIPANT -> throw UnsupportedOperationException("Bulk insert not supported for participants")
//            SectionEnum.EXPERIMENT -> throw UnsupportedOperationException("Bulk insert not supported for experiments")
//            SectionEnum.SESSION -> throw UnsupportedOperationException("Bulk insert not supported for sessions")
//            SectionEnum.SESSION_EVENT -> sessionEventDAO.bulkInsert(dtos as List<SessionEventDTO>)
//        }
//    }

    @Throws(DaoException::class)
    fun handleBulkInsertParticipants(dtos: List<ParticipantDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for participants")

    @Throws(DaoException::class)
    fun handleBulkInsertExperiments(dtos: List<ExperimentDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for experiments")

    @Throws(DaoException::class)
    fun handleBulkInsertSessions(dtos: List<SessionDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for sessions")

    @Throws(DaoException::class)
    fun handleBulkInsertSessionEvents(dtos: List<SessionEventDTO>): Int =
        sessionEventDAO.bulkInsert(dtos)

    @Throws(DaoException::class)
    fun handleExists(section: SectionEnum, pk: Any): Boolean {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.exists(pk as ParticipantPK)
            SectionEnum.EXPERIMENT -> experimentDAO.exists(pk as ExperimentPK)
            SectionEnum.SESSION -> sessionDAO.exists(pk as SessionPK)
            SectionEnum.SESSION_EVENT -> sessionEventDAO.exists(pk as SessionEventPK)
        }
    }

//    @Throws(DaoException::class)
//    fun handleDelete(section: SectionEnum, pk: Any) {
//        when (section) {
//            SectionEnum.PARTICIPANT -> {
//                val participantDTO = pk as ParticipantDTO
//                if (participantDTO.pid == null) throw Exception("A participant id was not provided for deletion")
//                participantDAO.delete(ParticipantPK(participantDTO.pid))
//            }
//            SectionEnum.EXPERIMENT -> {
//                val exp = pk as ExperimentDTO
//                if (exp.expId == null) throw Exception("An experiment id was not provided for deletion")
//                experimentDAO.delete(ExperimentPK(exp.expId))
//            }
//            SectionEnum.SESSION -> {
//                val sessionDTO = pk as SessionDTO
//                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for deletion")
//                sessionDAO.delete(SessionPK(sessionDTO.sessionId))
//            }
//            SectionEnum.SESSION_EVENT -> {
//                val sessionEventDTO = pk as SessionEventDTO
//                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for deletion")
//                sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
//            }
//        }
//    }

    @Throws(DaoException::class)
    fun handleDelete(participantDTO: ParticipantDTO) {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for deletion")
        participantDAO.delete(ParticipantPK(participantDTO.pid))
    }

    @Throws(DaoException::class)
    fun handleDelete(exp: ExperimentDTO) {
        if (exp.expId == null) throw Exception("An experiment id was not provided for deletion")
        experimentDAO.delete(ExperimentPK(exp.expId))
    }

    @Throws(DaoException::class)
    fun handleDelete(sessionDTO: SessionDTO) {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for deletion")
        sessionDAO.delete(SessionPK(sessionDTO.sessionId))
    }

    @Throws(DaoException::class)
    fun handleDelete(sessionEventDTO: SessionEventDTO) {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for deletion")
        sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
    }

//    @Throws(DaoException::class)
//    fun handleUpdate(section: SectionEnum, dto: Any) {
//        when (section) {
//            SectionEnum.PARTICIPANT -> {
//                val participantDTO = dto as ParticipantDTO
//                if (participantDTO.pid == null) throw Exception("A participant id was not provided for update")
//                participantDAO.update(participantDTO)
//            }
//            SectionEnum.EXPERIMENT -> {
//                val exp = dto as ExperimentDTO
//                if (exp.expId == null) throw Exception("An experiment id was not provided for update")
//                experimentDAO.update(exp)
//            }
//            SectionEnum.SESSION -> {
//                val sessionDTO = dto as SessionDTO
//                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for update")
//                sessionDAO.update(sessionDTO)
//            }
//            SectionEnum.SESSION_EVENT -> {
//                val sessionEventDTO = dto as SessionEventDTO
//                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for update")
//                sessionEventDAO.update(sessionEventDTO)
//            }
//        }
//    }

    @Throws(DaoException::class)
    fun handleUpdate(participantDTO: ParticipantDTO) {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for update")
        participantDAO.update(participantDTO)
    }

    @Throws(DaoException::class)
    fun handleUpdate(exp: ExperimentDTO) {
        if (exp.expId == null) throw Exception("An experiment id was not provided for update")
        experimentDAO.update(exp)
    }

    @Throws(DaoException::class)
    fun handleUpdate(sessionDTO: SessionDTO) {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for update")
        sessionDAO.update(sessionDTO)
    }

    @Throws(DaoException::class)
    fun handleUpdate(sessionEventDTO: SessionEventDTO) {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for update")
        sessionEventDAO.update(sessionEventDTO)
    }


//    @Throws(DaoException::class)
//    fun handleSelect(section: SectionEnum, pk: Any): Any {
//        return when (section) {
//            SectionEnum.PARTICIPANT -> {
//                val participantDTO = pk as ParticipantDTO
//                if (participantDTO.pid == null) throw Exception("A participant id was not provided for selection")
//                participantDAO.select(ParticipantPK(participantDTO.pid))
//            }
//            SectionEnum.EXPERIMENT -> {
//                val exp = pk as ExperimentDTO
//                if (exp.expId == null) throw Exception("An experiment id was not provided for selection")
//                experimentDAO.select(ExperimentPK(exp.expId))
//            }
//            SectionEnum.SESSION -> {
//                val sessionDTO = pk as SessionDTO
//                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for selection")
//                sessionDAO.select(SessionPK(sessionDTO.sessionId))
//            }
//            SectionEnum.SESSION_EVENT -> {
//                val sessionEventDTO = pk as SessionEventDTO
//                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for selection")
//                sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
//            }
//        }
//    }

    @Throws(DaoException::class)
    fun handleSelect(participantDTO: ParticipantDTO): Any {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for selection")
        return participantDAO.select(ParticipantPK(participantDTO.pid))
    }

    @Throws(DaoException::class)
    fun handleSelect(exp: ExperimentDTO): Any {
        if (exp.expId == null) throw Exception("An experiment id was not provided for selection")
        return experimentDAO.select(ExperimentPK(exp.expId))
    }

    @Throws(DaoException::class)
    fun handleSelect(sessionDTO: SessionDTO): Any {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for selection")
        return sessionDAO.select(SessionPK(sessionDTO.sessionId))
    }

    @Throws(DaoException::class)
    fun handleSelect(sessionEventDTO: SessionEventDTO): Any {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for selection")
        return sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
    }

    fun handleSelectAllParticipants(): List<ParticipantDTO> = participantDAO.selectAll()
    fun handleSelectAllExperiments(): List<ExperimentDTO> = experimentDAO.selectAll()
    fun handleSelectAllSessions(): List<SessionDTO> = sessionDAO.selectAll()
    fun handleSelectAllSessionEvents(): List<SessionEventDTO> = sessionEventDAO.selectAll()


    companion object {
        private val log = LoggerFactory.getLogger(DAOController::class.java)
    }
}
