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
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun handleParticipants(action: String,participant: Participant): String {
        when(action){
            "insert" -> {
                return Response.getOk(insert(participant))
            }
            "delete" -> {
                if(participant.pid == null)
                    throw Exception("A participant id was not provided for deletion")
                val ppk = ParticipantPK(participant.pid)
                delete(ppk)
                return Response.getOk()
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