class SessionEventsDAO(cursor: SQLExecutor) : DAOBase<SessionEvent, PrimaryKey>(cursor, "SessionEvents", arrayOf("event_id")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        val builder = CreateTableQueryBuilder("SessionEvents")
        builder.addColumn("session_id", "INT")
        builder.addColumn("type", "session_type_enum NOT NULL")
        builder.addColumn("subtype", "session_subtype_enum NOT NULL")
        builder.addColumn("timestamp", "BIGINT NOT NULL")
        builder.addColumn("actor", "VARCHAR(100) NOT NULL")
        builder.addColumn("data", "TEXT")
        builder.addForeignKey("session_id", "Sessions(session_id)")
        return builder
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionEvent {
        val eventId = resultSet.getInt("event_id")
        val sessionId = resultSet.getInt("session_id")
        val type = SessionType.valueOf(resultSet.getString("type"))
        val subtype = SessionSubType.valueOf(resultSet.getString("subtype"))
        val timestamp = resultSet.getLong("timestamp")
        val actor = resultSet.getString("actor")
        val data = resultSet.getString("data")

        return SessionEvent(eventId, sessionId, type, subtype, timestamp, actor, data)
    }
}
