package com.imsproject.watch.persistance

import com.imsproject.common.dataAccess.ColumnModifiers
import com.imsproject.common.dataAccess.ColumnType
import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.getTyped
import com.imsproject.common.gameserver.SessionEvent

class SessionEventDAO(
    val cursor: AndroidSQLiteExecutor
) : DAOBase<SessionEvent, SessionEventPrimaryKey>(cursor,"session_events",SessionEventPrimaryKey.columnNames()) {

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionEvent {
        val sessionId = resultSet.getTyped<String>("sessionId")!!
        val type = resultSet.getTyped<SessionEvent.Type>("type")!!
        val subtype = resultSet.getTyped<SessionEvent.SubType>("subtype")!!
        val timestamp = resultSet.getTyped<Long>("timestamp")!!
        val actor = resultSet.getTyped<String>("actor")!!
        val data = resultSet.getTyped<String>("data")
        return SessionEvent(sessionId, type, subtype, timestamp, actor, data)
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        return CreateTableQueryBuilder(tableName)
            .addColumn("sessionId", ColumnType.TEXT, modifiers = ColumnModifiers.PRIMARY_KEY)
            .addColumn("type", ColumnType.TEXT, modifiers = ColumnModifiers.PRIMARY_KEY)
            .addColumn("subtype", ColumnType.TEXT, modifiers = ColumnModifiers.PRIMARY_KEY)
            .addColumn("timestamp", ColumnType.INTEGER, modifiers = ColumnModifiers.PRIMARY_KEY)
            .addColumn("actor", ColumnType.TEXT, modifiers = ColumnModifiers.PRIMARY_KEY)
            .addColumn("data", ColumnType.TEXT)
    }

    override fun insert(obj: SessionEvent) {
        try{
            val query = "INSERT INTO $tableName (sessionId, type, subtype, timestamp, actor, data) VALUES (?, ?, ?, ?, ?, ?)"
            cursor.executeWrite(query, *(obj.toParams()))
        } catch (e: java.sql.SQLException) {
            throw DaoException("Failed to insert session event", e)
        }
    }

    override fun update(obj: SessionEvent) {
        val failMessage = "Failed to update session event"
        try{
            val query = "UPDATE $tableName SET data = ? WHERE sessionId = ? AND type = ? AND subtype = ? AND timestamp = ? AND actor = ?"
            cursor.executeWrite(query, *obj.toParams())
        } catch (e: java.sql.SQLException) {
            throw DaoException(failMessage, e)
        }
    }

    fun insertBulk(events: List<SessionEvent>) {
        try {
            val query = "INSERT INTO $tableName (sessionId, type, subtype, timestamp, actor, data) VALUES (?, ?, ?, ?, ?, ?)"
            val args = events.map {
                it.toParams()
            }
            cursor.executeBulkInsert(query, args)
        } catch (e: java.sql.SQLException) {
            throw DaoException("Failed to insert session events", e)
        }
    }

    private fun SessionEvent.toParams(): Array<out Any?> {
        return arrayOf(sessionId, type.name, subType.name, timestamp, actor, data)

    }

}