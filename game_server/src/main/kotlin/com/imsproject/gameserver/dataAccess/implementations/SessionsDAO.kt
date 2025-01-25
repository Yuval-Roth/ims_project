package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.SessionDTO


class SessionsDAO(cursor: SQLExecutor) : DAOBase<SessionDTO, SessionPK>(cursor, "Sessions", SessionPK.primaryColumnsList, arrayOf("exp_id", "duration", "session_type", "session_order", "tolerance", "window_length")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionDTO {
        return SessionDTO(
            sessionId = (resultSet.getObject("session_id") as? Int),
            expId = resultSet.getObject("exp_id") as? Int,
            duration = resultSet.getObject("duration") as? Int,
            sessionType = (resultSet.getObject("session_type") as? String),
            sessionOrder = resultSet.getObject("session_order") as? Int,
            tolerance = resultSet.getObject("tolerance") as? Int,
            windowLength = resultSet.getObject("window_length") as? Int
        )
    }

    fun handleSessions(action: String, sessionDTO: SessionDTO, transactionId: String? = null): String {
        when(action){
            "insert" -> {
                return Response.getOk(insert(sessionDTO, transactionId))
            }
            "delete" -> {
                if(sessionDTO.sessionId == null)
                    throw Exception("A session id was not provided for deletion")
                delete(SessionPK(sessionDTO.sessionId), transactionId)
                return Response.getOk()
            }
            "update" -> {
                if(sessionDTO.sessionId == null)
                    throw Exception("A session id was not provided for update")
                update(sessionDTO, transactionId)
                return Response.getOk()
            }
            "select" -> {
                if(sessionDTO.sessionId == null)
                    return Response.getOk(selectAll(transactionId))
                else
                    return Response.getOk(select(SessionPK(sessionDTO.sessionId), transactionId))
            }
            else -> throw Exception("Invalid action for sessions")
        }
    }


    @Throws(DaoException::class)
    override fun insert(obj: SessionDTO, transactionId: String?): Int {
        val values : Array<Any?>  = arrayOf(obj.expId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength)
        val idColName = SessionPK.primaryColumnsList.joinToString()
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: SessionDTO, transactionId: String?) {
        val values : Array<Any?> = arrayOf(obj.expId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength)
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