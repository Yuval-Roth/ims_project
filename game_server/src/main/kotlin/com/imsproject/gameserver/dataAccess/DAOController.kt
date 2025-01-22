package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess

import com.imsproject.common.utils.JsonUtils
import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.fromJson
import com.imsproject.gameserver.dataAccess.implementations.LobbiesDAO
import com.imsproject.gameserver.dataAccess.implementations.SessionsDAO
import com.imsproject.gameserver.dataAccess.models.Lobby
//import com.imsproject.gameserver.dataAccess.implementations.LobbiesDAO
//import com.imsproject.gameserver.dataAccess.implementations.SessionsDAO
//import com.imsproject.gameserver.dataAccess.implementations.SessionEventsDAO
import com.imsproject.gameserver.dataAccess.models.Participant
import com.imsproject.gameserver.dataAccess.models.Session
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "localhost", 5432 ,"ims-db", "admin", "adminMTAC"
    )

// seperate cursor to each
    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
    val lobbyDAO: LobbiesDAO = LobbiesDAO(cursor)
    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
//    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)


    // todo: (later) add cotrollers for input check
    // todo: (now) change add lobby to be with session list.
    @Throws(SQLException::class)
    fun handle(section: String, action: String ,body : String): String {
        when (section) { // unserialized data can be
            "participant" -> { //return id from db
                val participant: Participant = fromJson(body)
                return participantDAO.handleParticipants(action, participant)
            }
            "lobby" -> {
                val lobby: Lobby = fromJson(body)
                return lobbyDAO.handleLobbies(action, lobby)
            }
            "session" -> {
                val session: Session = fromJson(body)
                return sessionDAO.handleSessions(action, session)
            }

            else -> throw (SQLException("horrible stuff happened"))
        }
    }

    //create logger like in gamecotroller (something companion)
}
