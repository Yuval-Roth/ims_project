class ParticipantsDAO(cursor: SQLExecutor) : DAOBase<Participant, Int>(cursor, "Participants", arrayOf("pid")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        return CreateTableQueryBuilder("Participants")
            .addColumn("pid", "SERIAL PRIMARY KEY")
            .addColumn("first_name", "VARCHAR(50) NOT NULL")
            .addColumn("last_name", "VARCHAR(50) NOT NULL")
            .addColumn("age", "INT")
            .addColumn("gender", "gender_enum")
            .addColumn("phone", "VARCHAR(15)")
            .addColumn("email", "VARCHAR(100) UNIQUE")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Participant {
        return Participant(
            pid = resultSet.getInt("pid"),
            firstName = resultSet.getString("first_name"),
            lastName = resultSet.getString("last_name"),
            age = resultSet.getIntOrNull("age"),
            gender = resultSet.getEnum("gender", GenderEnum::class),
            phone = resultSet.getStringOrNull("phone"),
            email = resultSet.getString("email")
        )
    }
}
