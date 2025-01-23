package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.common.utils.fromJson

import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
import com.imsproject.gameserver.dataAccess.implementations.ExperimentsDAO
import com.imsproject.gameserver.dataAccess.implementations.SessionsDAO
import com.imsproject.gameserver.dataAccess.implementations.SessionEventsDAO
import com.imsproject.gameserver.dataAccess.models.*

import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "localhost", 5432 ,"ims-db", "admin", "adminMTAC"
    )

    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
    val experimentDAO: ExperimentsDAO = ExperimentsDAO(cursor)
    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)


    // todo: (later) add cotrollers for input check
    @Throws(SQLException::class)
    fun handle(section: String, action: String ,body : String): String {
        when (section) {
            "participant" -> {
                val participantDTO: ParticipantDTO = fromJson(body)
                return participantDAO.handleParticipants(action, participantDTO)
            }
            "experiment" -> {
                val exp: ExperimentDTO = fromJson(body)
                return experimentDAO.handleExperiments(action, exp)
            }
            "session" -> {
                val sessionDTO: SessionDTO = fromJson(body)
                return sessionDAO.handleSessions(action, sessionDTO)
            }
            "sessionEvent" -> {
                val sessionEventDTO: SessionEventDTO = fromJson(body)
                return sessionEventDAO.handleSessionEvents(action, sessionEventDTO)
                //todo: add bulk session addition
            }
            "experimentSession" -> {
                return handleExperimentSession(action, body)
            }

            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    @Throws(SQLException::class)
    fun handleExperimentSession(action: String ,body : String): String {
        when (action) {
            "insert" -> {
                try {
                    val tid = cursor.beginTransaction()
                    val esdata: ExpWithSessionsData = fromJson(body)
                    val expdto = ExperimentDTO(expId = null, pid1 = esdata.pid1, pid2 = esdata.pid2)
                    val response: Response = fromJson(experimentDAO.handleExperiments(action, expdto, tid))
                    val expId: Int = response.payload!![0].toInt()

                    val sessionIds: MutableMap<String, Int> = mutableMapOf("expId" to expId)
                    for (s in esdata.sessions) {
                        val sdto: SessionDTO = SessionDTO.create(expId, s)
                        val response2: Response = fromJson(sessionDAO.handleSessions(action, sdto, tid))
                        val sessId: Int = response2.payload!![0].toInt()
                        sessionIds[sdto.sessionOrder.toString()] = sessId
                    }

                    cursor.commit(tid)
                    return Response.getOk(sessionIds)
                } catch (e: Exception) {
                    //rollback automatic
                    throw SQLException("""Insert Experiment and Sessions doesn't work""")
                }
            }
            else -> throw (SQLException("Unknown section '$action'"))
        }
    }


        //create logger like in gamecotroller (something companion)
}
