package com.imsproject.gameserver.dataAccess.implementations

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

    fun handleParticipants(action: string,participant: Participant): Unit {
        when(action){
            "insert" -> {
                val participant : Participant = JsonUtils.deserialize(body)
                insert(participant)
            }
            else -> throw Exception("Invalid action for participants")
        }

    }

    @Throws(DaoException::class)
    fun insert(participant: Participant): Unit {
            val columns = arrayOf("first_name", "last_name", "age", "gender", "phone", "email")
            val values = "'" + participant.first_name + "', '" + participant.last_name + "', " + participant.age + ", '" + participant.gender + "', '" + participant.phone + "', '" + participant.email + "'"
            val insertQuery = "INSERT INTO ${tableName} (${columns.joinToString(", ")}) VALUES (${values});"
            try {
                cursor.executeWrite(insertQuery)
            } catch (e: SQLException) {
                throw DaoException("Failed to delete from table $tableName", e)
            }
    }

}
