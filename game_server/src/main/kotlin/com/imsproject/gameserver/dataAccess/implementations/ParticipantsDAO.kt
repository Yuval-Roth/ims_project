package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.ParticipantDTO
import org.springframework.stereotype.Component

@Component
class ParticipantsDAO(cursor: SQLExecutor) : DAOBase<ParticipantDTO, ParticipantPK>(cursor, "Participants", ParticipantPK.primaryColumnsList, arrayOf("first_name", "last_name", "age", "gender", "phone", "email")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): ParticipantDTO {
        return ParticipantDTO(
            pid = resultSet.getInt("pid"),
            firstName = resultSet.getString("first_name"),
            lastName = resultSet.getString("last_name"),
            age = resultSet.getInt("age"),
            gender = resultSet.getString("gender"),
            phone = resultSet.getString("phone"),
            email = resultSet.getString("email")
        )
    }


    @Throws(DaoException::class)
    override fun insert(obj: ParticipantDTO, transactionId: String?): Int {
            val values : Array<Any?> = arrayOf(obj.firstName,obj.lastName,obj.age,obj.gender,obj.phone,obj.email)
            val idColName = ParticipantPK.primaryColumnsList.joinToString()
            return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: ParticipantDTO, transactionId: String?) {
        val values : Array<Any?> = arrayOf(obj.firstName,obj.lastName,obj.age,obj.gender,obj.phone,obj.email)
        val id = obj.pid ?: throw IllegalArgumentException("Participant ID (pid) must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, values, transactionId)
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