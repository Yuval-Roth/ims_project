package com.imsproject.gameserver.dataAccess

class DAOController {
    private val cursor: SQLExecutor = PostgreSQLExecutor(
        "db-server", "ims-db", "admin", "adminMTAC"
    )

    val participantDAO: ParticipantsDAO = ParticipantDAOImpl(cursor)
    val lobbyDAO: LobbiesDAO = LobbyDAOImpl(cursor)
    val sessionDAO: SessionsDAO = SessionDAOImpl(cursor)
    val sessionEventDAO: SessionEventDAO = SessionEventUserInputDAOImpl(cursor)

    // *************************************
    // route the request to the relevant DAO
    // *************************************
    fun handleParticipants(action: string,participant: Participant): Unit {
        lobbyDAO.handle(action, participant);
    }

    // *************************************
    // if not exist, initialize tables
    // *************************************

}
