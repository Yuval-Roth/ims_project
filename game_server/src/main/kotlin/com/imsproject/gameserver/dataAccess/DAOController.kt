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
        "localhost", 5432 ,"ims-db", "admin", "adminMTAC"
    )

    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
    val experimentDAO: ExperimentsDAO = ExperimentsDAO(cursor)
    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)

    @Throws(SQLException::class)
    fun handleInsert(section: SectionEnum ,dto : Any): Int {
        when (section) {
            SectionEnum.PARTICIPANT -> {
                return participantDAO.insert(dto as ParticipantDTO)
            }
            SectionEnum.EXPERIMENT -> {
                return experimentDAO.insert(dto as ExperimentDTO)
            }
            SectionEnum.SESSION -> {
                return sessionDAO.insert(dto as SessionDTO)
            }
            SectionEnum.SESSIONEVENT -> {
                return sessionEventDAO.insert(dto as SessionEventDTO)
                //todo: add bulk session addition
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleExists(section: String ,pk : Any): Boolean {
        when (section) {
            "participant" -> {
                return participantDAO.exists(pk as ParticipantPK)
            }
            "experiment" -> {
                return experimentDAO.exists(pk as ExperimentPK)
            }
            "session" -> {
                return sessionDAO.exists(pk as SessionPK)
            }
            "sessionEvent" -> {
                return sessionEventDAO.exists(pk as SessionEventPK)
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleDelete(section: String ,dto : Any): Unit {
        when (section) {
            "participant" -> {
                val participantDTO: ParticipantDTO = dto as ParticipantDTO
                if(participantDTO.pid == null)
                    throw Exception("A participant id was not provided for deletion")
                participantDAO.delete(ParticipantPK(participantDTO.pid))
            }
            "experiment" -> {
                val exp: ExperimentDTO = dto as ExperimentDTO
                if(exp.expId == null)
                    throw Exception("An experiment id was not provided for deletion")
                experimentDAO.delete(ExperimentPK(exp.expId))
            }
            "session" -> {
                val sessionDTO: SessionDTO = dto as SessionDTO
                if(sessionDTO.sessionId == null)
                    throw Exception("A session id was not provided for deletion")
                sessionDAO.delete(SessionPK(sessionDTO.sessionId))
            }
            "sessionEvent" -> {
                val sessionEventDTO: SessionEventDTO = dto as SessionEventDTO
                if(sessionEventDTO.eventId == null)
                    throw Exception("A session event id was not provided for deletion")
                sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleUpdate(section: String ,dto : Any): Unit {
        when (section) {
            "participant" -> {
                val participantDTO: ParticipantDTO = dto as ParticipantDTO
                if(participantDTO.pid == null)
                    throw Exception("A participant id was not provided for update")
                participantDAO.update(participantDTO)

            }
            "experiment" -> {
                val exp: ExperimentDTO = dto as ExperimentDTO
                if(exp.expId == null)
                    throw Exception("An experiment id was not provided for update")
                experimentDAO.update(exp)
            }
            "session" -> {
                val sessionDTO: SessionDTO = dto as SessionDTO
                if(sessionDTO.sessionId == null)
                    throw Exception("A session id was not provided for update")
                sessionDAO.update(sessionDTO)
            }
            "sessionEvent" -> {
                val sessionEventDTO: SessionEventDTO = dto as SessionEventDTO
                if(sessionEventDTO.eventId == null)
                    throw Exception("A session event id was not provided for update")
                sessionEventDAO.update(sessionEventDTO)
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleSelect(section: String ,dto : Any): Any {
        when (section) {
            "participant" -> {
                val participantDTO: ParticipantDTO = dto as ParticipantDTO
                if (participantDTO.pid == null)
                    throw Exception("A participant id was not provided for selection")
                return participantDAO.select(ParticipantPK(participantDTO.pid))
            }

            "experiment" -> {
                val exp: ExperimentDTO = dto as ExperimentDTO
                if (exp.expId == null)
                    throw Exception("An experiment id was not provided for selection")
                return experimentDAO.select(ExperimentPK(exp.expId))
            }

            "session" -> {
                val sessionDTO: SessionDTO = dto as SessionDTO
                if (sessionDTO.sessionId == null)
                    throw Exception("A session id was not provided for selection")
                return sessionDAO.select(SessionPK(sessionDTO.sessionId))
            }

            "sessionEvent" -> {
                val sessionEventDTO: SessionEventDTO = dto as SessionEventDTO
                if (sessionEventDTO.eventId == null)
                    throw Exception("A session event id was not provided for selection")
                return sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleSelectAll(section: String): List<Any> {
        when (section) {
            "participant" -> {
                return participantDAO.selectAll()
            }
            "experiment" -> {
                return experimentDAO.selectAll()
            }
            "session" -> {
                return sessionDAO.selectAll()
            }
            "sessionEvent" -> {
                return sessionEventDAO.selectAll()
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }


    @Throws(SQLException::class)
    fun insertExperimentSession(body : String): MutableMap<String, Int> {
        try {
            val tid = cursor.beginTransaction()
            val esdata: ExpWithSessionsData = fromJson(body)
            val expdto = ExperimentDTO(expId = null, pid1 = esdata.pid1, pid2 = esdata.pid2)
            val expId: Int = experimentDAO.insert(expdto, tid) //performing experiments insertion

            val sessionIds: MutableMap<String, Int> = mutableMapOf("expId" to expId)
            for (s in esdata.sessions) {
                val sdto: SessionDTO = SessionDTO.create(expId, s)
                val sessId: Int = sessionDAO.insert(sdto, tid)
                sessionIds[sdto.sessionOrder.toString()] = sessId
            }

            cursor.commit(tid)
            return sessionIds
        } catch (e: Exception) {
            //rollback automatic
            throw SQLException("""Insert Experiment and Sessions doesn't work""")
        }
    }

        //create logger like in gamecotroller (something companion)
}
