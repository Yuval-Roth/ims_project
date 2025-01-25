package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.fromJson
import com.imsproject.gameserver.dataAccess.implementations.*

import com.imsproject.gameserver.dataAccess.models.*

import org.springframework.stereotype.Component
import java.sql.SQLException

enum class SectionEnum {
    PARTICIPANT, EXPERIMENT, SESSION, SESSIONEVENT
}

@Component
class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "localhost", 5432, "ims-db", "admin", "adminMTAC"
    )

    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
    val experimentDAO: ExperimentsDAO = ExperimentsDAO(cursor)
    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)

    @Throws(SQLException::class)
    fun handleInsert(section: SectionEnum, dto: Any): Int {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.insert(dto as ParticipantDTO)
            SectionEnum.EXPERIMENT -> experimentDAO.insert(dto as ExperimentDTO)
            SectionEnum.SESSION -> sessionDAO.insert(dto as SessionDTO)
            SectionEnum.SESSIONEVENT -> sessionEventDAO.insert(dto as SessionEventDTO)
        } //todo: add bulk insert for session events
    }

    @Throws(SQLException::class)
    fun handleExists(section: SectionEnum, pk: Any): Boolean {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.exists(pk as ParticipantPK)
            SectionEnum.EXPERIMENT -> experimentDAO.exists(pk as ExperimentPK)
            SectionEnum.SESSION -> sessionDAO.exists(pk as SessionPK)
            SectionEnum.SESSIONEVENT -> sessionEventDAO.exists(pk as SessionEventPK)
        }
    }

    @Throws(SQLException::class)
    fun handleDelete(section: SectionEnum, dto: Any) {
        when (section) {
            SectionEnum.PARTICIPANT -> {
                val participantDTO = dto as ParticipantDTO
                if (participantDTO.pid == null) throw Exception("A participant id was not provided for deletion")
                participantDAO.delete(ParticipantPK(participantDTO.pid))
            }
            SectionEnum.EXPERIMENT -> {
                val exp = dto as ExperimentDTO
                if (exp.expId == null) throw Exception("An experiment id was not provided for deletion")
                experimentDAO.delete(ExperimentPK(exp.expId))
            }
            SectionEnum.SESSION -> {
                val sessionDTO = dto as SessionDTO
                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for deletion")
                sessionDAO.delete(SessionPK(sessionDTO.sessionId))
            }
            SectionEnum.SESSIONEVENT -> {
                val sessionEventDTO = dto as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for deletion")
                sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
            }
        }
    }

    @Throws(SQLException::class)
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
            SectionEnum.SESSIONEVENT -> {
                val sessionEventDTO = dto as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for update")
                sessionEventDAO.update(sessionEventDTO)
            }
        }
    }

    @Throws(SQLException::class)
    fun handleSelect(section: SectionEnum, dto: Any): Any {
        return when (section) {
            SectionEnum.PARTICIPANT -> {
                val participantDTO = dto as ParticipantDTO
                if (participantDTO.pid == null) throw Exception("A participant id was not provided for selection")
                participantDAO.select(ParticipantPK(participantDTO.pid))
            }
            SectionEnum.EXPERIMENT -> {
                val exp = dto as ExperimentDTO
                if (exp.expId == null) throw Exception("An experiment id was not provided for selection")
                experimentDAO.select(ExperimentPK(exp.expId))
            }
            SectionEnum.SESSION -> {
                val sessionDTO = dto as SessionDTO
                if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for selection")
                sessionDAO.select(SessionPK(sessionDTO.sessionId))
            }
            SectionEnum.SESSIONEVENT -> {
                val sessionEventDTO = dto as SessionEventDTO
                if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for selection")
                sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
            }
        }
    }

    @Throws(SQLException::class)
    fun handleSelectAll(section: SectionEnum): List<Any> {
        return when (section) {
            SectionEnum.PARTICIPANT -> participantDAO.selectAll()
            SectionEnum.EXPERIMENT -> experimentDAO.selectAll()
            SectionEnum.SESSION -> sessionDAO.selectAll()
            SectionEnum.SESSIONEVENT -> sessionEventDAO.selectAll()
        }
    }

    //create logger like in gamecotroller (something companion)
}
