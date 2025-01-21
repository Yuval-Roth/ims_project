package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.Participant

import java.sql.SQLException

class ParticipantsDAO(cursor: SQLExecutor) : DAOBase<Participant, ParticipantPK>(cursor, "Participants", ParticipantPK.primaryColumnsList) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Participant {
        return Participant(
            pid = (resultSet.getObject("pid") as? Int),
            firstName = resultSet.getObject("first_name") as? String,
            lastName = resultSet.getObject("last_name") as? String,
            age = (resultSet.getObject("age") as? Int),
            gender = resultSet.getObject("gender") as? String,
            phone = resultSet.getObject("phone") as? String,
            email = resultSet.getObject("email") as? String
        )
    }

    fun handleParticipants(action: String,participant: Participant): String {
        when(action){
            "insert" -> {
                return Response.getOk(insert(participant))
            }
            "delete" -> {
                if(participant.pid == null)
                    throw Exception("A participant id was not provided for deletion")
                delete(ParticipantPK(participant.pid))
                return Response.getOk()
            }
            "update" -> {
                if(participant.pid == null)
                    throw Exception("A participant id was not provided for update")
                update(participant)
                return Response.getOk()
            }
            "select" -> {
                if(participant.pid == null)
                    return Response.getOk(selectAll())
                else
                    return Response.getOk(select(ParticipantPK(participant.pid)))
            }
            else -> throw Exception("Invalid action for participants")
        }
    }

    @Throws(DaoException::class)
    override fun insert(obj: Participant): Int {
            val columns = arrayOf("first_name", "last_name", "age", "gender", "phone", "email")
            val values = arrayOf(obj.firstName,obj.lastName,obj.age,obj.gender,obj.phone,obj.email)
            val insertQuery = "INSERT INTO $tableName (${columns.joinToString()}) VALUES (?, ?, ?, ?, ?, ?) RETURNING pid"
            try {
                val keysResultSet = cursor.executeInsert(insertQuery,*values)
                if(keysResultSet.next()) {
                    val pid = keysResultSet.getTyped<Int>("pid")
                    if(pid != null){
                        return pid;
                    }
                }
            } catch (e: SQLException) {
                throw DaoException("Failed to insert to table $tableName", e)
            }
        throw DaoException("Error in insertion to $tableName")
    }

    @Throws(DaoException::class)
    override fun update(obj: Participant): Unit {
        try {
            // Validate ID
            val id = obj.pid ?: throw IllegalArgumentException("Participant ID (pid) must not be null")

            // Build the query dynamically
            val updates = mutableListOf<String>()
            val params = mutableListOf<Any?>()

            val fields = mapOf(
                "first_name" to obj.firstName,
                "last_name" to obj.lastName,
                "age" to obj.age,
                "gender" to obj.gender,
                "phone" to obj.phone,
                "email" to obj.email
            )

            fields.forEach { (column, value) ->
                value?.let {
                    updates.add("$column = ?")
                    params.add(it)
                }
            }

            if (updates.isEmpty()) {
                throw IllegalArgumentException("No fields to update")
            }

            val query = "UPDATE participants SET ${updates.joinToString(", ")} WHERE pid = ?"
            params.add(id)

            // Execute the query
            cursor.executeWrite(query, *params.toTypedArray())
        } catch (e: SQLException) {
            throw DaoException("Failed to update table $tableName", e)
        }
    }
}

data class ParticipantPK(
    val pid: Int // Primary key column in the "Participants" table
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("pid")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "pid" -> pid
            else -> null
        }
    }
}