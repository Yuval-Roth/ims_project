package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.Session
import java.sql.SQLException


class SessionsDAO(cursor: SQLExecutor) : DAOBase<Session, SessionPK>(cursor, "Sessions", SessionPK.primaryColumnsList, arrayOf("lobby_id", "duration", "session_type", "session_order", "tolerance", "window_length")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Session {
        return Session(
            sessionId = (resultSet.getObject("session_id") as? Int),
            lobbyId = resultSet.getObject("lobby_id") as? Int,
            duration = resultSet.getObject("duration") as? Int,
            sessionType = (resultSet.getObject("session_type") as? String),
            sessionOrder = resultSet.getObject("session_order") as? Int,
            tolerance = resultSet.getObject("tolerance") as? Int,
            windowLength = resultSet.getObject("window_length") as? Int
        )
    }

    fun handleSessions(action: String,session: Session): String {
        when(action){
            "insert" -> {
                return Response.getOk(insert(session))
            }
            "delete" -> {
                if(session.sessionId == null)
                    throw Exception("A session id was not provided for deletion")
                delete(SessionPK(session.sessionId))
                return Response.getOk()
            }
            "update" -> {
                if(session.sessionId == null)
                    throw Exception("A session id was not provided for update")
                update(session)
                return Response.getOk()
            }
            "select" -> {
                if(session.sessionId == null)
                    return Response.getOk(selectAll())
                else
                    return Response.getOk(select(SessionPK(session.sessionId)))
            }
            else -> throw Exception("Invalid action for sessions")
        }
    }


    @Throws(DaoException::class)
    override fun insert(obj: Session): Int {
        val values = arrayOf(obj.lobbyId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength)
        val idColName = SessionPK.primaryColumnsList.joinToString()
        return buildQueryAndInsert(idColName, *values)
    }

    @Throws(DaoException::class)
    override fun update(obj: Session): Unit {
        val values = arrayOf(obj.lobbyId,obj.duration,obj.sessionType,obj.sessionOrder,obj.tolerance,obj.windowLength)
        val id = obj.sessionId ?: throw IllegalArgumentException("session ID must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, *values)
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