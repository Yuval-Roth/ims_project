class DAOController {
    private val cursor: SQLExecutor = SQLExecutor()

    val participantDAO: ParticipantsDAO = ParticipantDAOImpl(cursor)
    val lobbyDAO: LobbyDAO = LobbyDAOImpl(cursor)
    val sessionDAO: SessionDAO = SessionDAOImpl(cursor)
    val sessionEventUserInputDAO: SessionEventUserInputDAO = SessionEventUserInputDAOImpl(cursor)

    // *************************************
    // route the request to the relevant DAO
    // *************************************

    // *************************************
    // if not exist, initialize tables
    // *************************************

}
