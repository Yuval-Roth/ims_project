class SessionsDAO(cursor: SQLExecutor) : DAOBase<Session, PrimaryKey>(cursor, "Sessions", arrayOf("session_id")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        val builder = CreateTableQueryBuilder("Sessions")
        builder.addColumn("lobby_id", "INT")
        builder.addColumn("duration", "INT")
        builder.addColumn("session_type", "session_type_enum")
        builder.addColumn("session_order", "INT")
        builder.addForeignKey("lobby_id", "Lobbies(lobby_id)")
        return builder
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Session {
        val sessionId = resultSet.getInt("session_id")
        val lobbyId = resultSet.getInt("lobby_id")
        val duration = resultSet.getInt("duration")
        val sessionType = SessionType.valueOf(resultSet.getString("session_type"))
        val sessionOrder = resultSet.getInt("session_order")

        return Session(sessionId, lobbyId, duration, sessionType, sessionOrder)
    }
}
