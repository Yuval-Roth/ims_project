package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.Participant
import java.sql.SQLException

class ParticipantsDAO(cursor: SQLExecutor) : DAOBase<Participant, PrimaryKey>(cursor, "Participants", arrayOf("pid")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Participant {
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun handleParticipants(action: String,participant: Participant): Unit {
        when(action){
            "insert" -> {
                insert(participant)
            }
            else -> throw Exception("Invalid action for participants")
        }
    }

    @Throws(DaoException::class)
    override fun insert(obj: Participant): Unit {
            val columns = arrayOf("first_name", "last_name", "age", "gender", "phone", "email")
            val values = "'" + obj.firstName + "', '" + obj.lastName + "', " + obj.age + ", '" + obj.gender + "', '" + obj.phone + "', '" + obj.email + "'"
            val insertQuery = "INSERT INTO ${tableName} (${columns.joinToString(", ")}) VALUES (${values}) RETURNING pid;"
            try {
                cursor.executeWrite(insertQuery) // returns number of lines changed. can be used to check for errors. log if anything
                //if succeed return id somehow
                // todo: get back to it, added "RETURNING pid", need to change executeWrite to retrieve the id.
            } catch (e: SQLException) {
                throw DaoException("Failed to insert to table $tableName", e)
            }
    }

    @Throws(DaoException::class)
    override fun update(participant: Participant): Unit {

    }
}
