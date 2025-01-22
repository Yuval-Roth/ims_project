package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.fromJson

import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
import com.imsproject.gameserver.dataAccess.implementations.ExperimentsDAO
import com.imsproject.gameserver.dataAccess.implementations.SessionsDAO
import com.imsproject.gameserver.dataAccess.implementations.SessionEventsDAO

import com.imsproject.gameserver.dataAccess.models.Participant
import com.imsproject.gameserver.dataAccess.models.Experiment
import com.imsproject.gameserver.dataAccess.models.Session
import com.imsproject.gameserver.dataAccess.models.SessionEvent

import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "localhost", 5432 ,"ims-db", "admin", "adminMTAC"
    )

// todo: seperate cursor to each
    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
    val experimentDAO: ExperimentsDAO = ExperimentsDAO(cursor)
    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)


    // todo: (later) add cotrollers for input check
    // todo: (now) change add lobby to be with session list.
    @Throws(SQLException::class)
    fun handle(section: String, action: String ,body : String): String {
        when (section) {
            "participant" -> {
                val participant: Participant = fromJson(body)
                return participantDAO.handleParticipants(action, participant)
            }
            "experiment" -> {
                val exp: Experiment = fromJson(body)
                return experimentDAO.handleLobbies(action, exp)
            }
            "session" -> {
                val session: Session = fromJson(body)
                return sessionDAO.handleSessions(action, session)
            }
            "sessionEvent" -> {
                val sessionEvent: SessionEvent = fromJson(body)
                return sessionEventDAO.handleSessionEvents(action, sessionEvent)
            }
            else -> throw (SQLException("Unknown section '$section'"))
        }
    }

    //create logger like in gamecotroller (something companion)
}
