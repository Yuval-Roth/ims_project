package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.JsonUtils
import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
//import com.imsproject.gameserver.dataAccess.implementations.LobbiesDAO
//import com.imsproject.gameserver.dataAccess.implementations.SessionsDAO
//import com.imsproject.gameserver.dataAccess.implementations.SessionEventsDAO
import com.imsproject.gameserver.dataAccess.models.Participant
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "db-server", 5432 ,"ims-db", "admin", "adminMTAC"
    )
// seperate cursor to each
    val participantDAO: ParticipantsDAO = ParticipantsDAO(cursor)
//    val lobbyDAO: LobbiesDAO = LobbiesDAO(cursor)
//    val sessionDAO: SessionsDAO = SessionsDAO(cursor)
//    val sessionEventDAO: SessionEventsDAO = SessionEventsDAO(cursor)

    // *************************************
    // route the request to the relevant DAO
    // *************************************
    @Throws(SQLException::class)
    fun handle(section: String, action: String ,body : String): Unit {
//        participantDAO.handle(action, participant);
        when (section) { // unserialized data can be
            "participant" -> { //return id from db
                val participant: Participant = JsonUtils.deserialize(body)
                participantDAO.handleParticipants(action, participant)
            }

            else -> throw (SQLException("horrible stuff happened"))
        }
    }

            // *************************************
    // if not exist, initialize tables
    // *************************************

    //create logger like in gamecotroller (something companion)
}
