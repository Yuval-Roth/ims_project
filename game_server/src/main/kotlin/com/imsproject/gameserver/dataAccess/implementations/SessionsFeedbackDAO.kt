package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.models.*
import org.springframework.stereotype.Component

@Component
class SessionsFeedbackDAO(cursor: SQLExecutor) : DAOBase<SessionFeedbackDTO, SessionFeedbackPK>(cursor, "SessionsFeedback", ExperimentPK.primaryColumnsList, arrayOf("session_id", "pid", "question", "answer")) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): SessionFeedbackDTO {
        return SessionFeedbackDTO(
            sessionId = resultSet.getInt("session_id"),
            pid = resultSet.getInt("pid"),
            question = resultSet.getString("question"),
            answer = resultSet.getString("answer")
        )
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @Throws(DaoException::class)
    override fun insert(obj: SessionFeedbackDTO, transactionId: String?): Int {
        val values = arrayOf<Any?>(obj.sessionId,obj.pid,obj.question,obj.answer)
        val idColName = "session_id" //unnecessary
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: SessionFeedbackDTO, transactionId: String?) {
        throw UnsupportedOperationException("Update of session feedback is not supported.")
    }

    fun bulkInsert(objs: List<SessionFeedbackDTO>, transactionId: String? = null): Int {
        val values : List<Array<Any?>> = objs.map { arrayOf(it.sessionId,it.pid,it.question,it.answer) }
        return buildQueryAndBulkInsert(values, transactionId)
    }
}

data class SessionFeedbackPK(
    val sessionId: Int?,
    val pid: Int,
    val question: String
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("session_id", "pid", "question")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "session_id" -> sessionId
            "pid" -> pid
            "question" -> question
            else -> null
        }
    }
}