package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.utils.Response

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.dataAccess.models.SessionEventDTO

class SessionEventsDAO(cursor: SQLExecutor) : DAOBase<SessionEventDTO, SessionEventPK>(cursor, "SessionEvents", SessionEventPK.primaryColumnsList, arrayOf("session_id", "type", "subtype", "timestamp", "actor", "data")) {
    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionEventDTO {
        return SessionEventDTO(
            eventId = (resultSet.getObject("event_id") as? Int),
            sessionId = resultSet.getObject("session_id") as? Int,
            type = resultSet.getObject("type") as? String,
            subtype = (resultSet.getObject("subtype") as? String),
            timestamp = resultSet.getObject("timestamp") as? Long,
            actor = resultSet.getObject("actor") as? String,
            data = resultSet.getObject("data") as? String
        )
    }

    fun handleSessionEvents(action: String, sessionEventDTO: SessionEventDTO, transactionId: String? = null): String {
        when(action){
            "insert" -> {
                return Response.getOk(insert(sessionEventDTO, transactionId))
            }
            "delete" -> {
                if(sessionEventDTO.eventId == null)
                    throw Exception("A session event id was not provided for deletion")
                delete(SessionEventPK(sessionEventDTO.eventId), transactionId)
                return Response.getOk()
            }
            "update" -> {
                if(sessionEventDTO.eventId == null)
                    throw Exception("A session event id was not provided for update")
                update(sessionEventDTO, transactionId)
                return Response.getOk()
            }
            "select" -> {
                if(sessionEventDTO.eventId == null)
                    return Response.getOk(selectAll(transactionId))
                else
                    return Response.getOk(select(SessionEventPK(sessionEventDTO.eventId), transactionId))
            }
            else -> throw Exception("Invalid action for session events")
        }
    }

    @Throws(DaoException::class)
    override fun insert(obj: SessionEventDTO, transactionId: String?): Int {
        val values = arrayOf(obj.sessionId,obj.type,obj.subtype,obj.timestamp,obj.actor,obj.data)
        val idColName = SessionEventPK.primaryColumnsList.joinToString()
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: SessionEventDTO, transactionId: String?) {
        val values = arrayOf(obj.sessionId,obj.type,obj.subtype,obj.timestamp,obj.actor,obj.data)
        val id = obj.eventId ?: throw IllegalArgumentException("Session Event ID  must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, values, transactionId)
    }
}

data class SessionEventPK(
    val event_id: Int
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("event_id")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "event_id" -> event_id
            else -> null
        }
    }
}

