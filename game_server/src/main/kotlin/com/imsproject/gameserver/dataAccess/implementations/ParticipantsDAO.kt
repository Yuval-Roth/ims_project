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

class ParticipantsDAO(cursor: SQLExecutor) : DAOBase<Participant, ParticipantPK>(cursor, "Participants", ParticipantPK.primaryColumnsList, arrayOf("first_name", "last_name", "age", "gender", "phone", "email")) {
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
            val values = arrayOf(obj.firstName,obj.lastName,obj.age,obj.gender,obj.phone,obj.email)
            val idColName = ParticipantPK.primaryColumnsList.joinToString()
            return buildQueryAndInsert(idColName, *values)
    }

    @Throws(DaoException::class)
    override fun update(obj: Participant): Unit {
        val values = arrayOf(obj.firstName,obj.lastName,obj.age,obj.gender,obj.phone,obj.email)
        val id = obj.pid ?: throw IllegalArgumentException("Participant ID (pid) must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, *values)
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