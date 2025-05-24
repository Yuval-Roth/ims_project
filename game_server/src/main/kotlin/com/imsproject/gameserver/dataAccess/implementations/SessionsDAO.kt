package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.SessionDTO
import org.springframework.stereotype.Component

@Component
class SessionsDAO(cursor: SQLExecutor) : DAOBase<SessionDTO, SessionPK>(cursor, "Sessions", SessionPK.primaryColumnsList, arrayOf("exp_id", "duration", "session_type", "session_order", "tolerance", "window_length","state")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionDTO {
        return SessionDTO(
            sessionId = resultSet.getInt("session_id"),
            expId = resultSet.getInt("exp_id"),
            duration = resultSet.getInt("duration"),
            sessionType = resultSet.getString("session_type"),
            sessionOrder = resultSet.getInt("session_order"),
            tolerance = resultSet.getInt("tolerance"),
            windowLength = resultSet.getInt("window_length"),
            state = resultSet.getString("state")
        )
    }

    @Throws(DaoException::class)
    override fun insert(obj: SessionDTO, transactionId: String?): Int {
        val values : Array<Any?>  = arrayOf(obj.expId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength,obj.state)
        val idColName = SessionPK.primaryColumnsList.joinToString()
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: SessionDTO, transactionId: String?) {
        val values : Array<Any?> = arrayOf(obj.expId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength,obj.state)
        val id = obj.sessionId ?: throw IllegalArgumentException("session ID must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, values, transactionId)
    }
}


data class SessionPK(
    val session_id: Int
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("session_id")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "session_id" -> session_id
            else -> null
        }
    }
}