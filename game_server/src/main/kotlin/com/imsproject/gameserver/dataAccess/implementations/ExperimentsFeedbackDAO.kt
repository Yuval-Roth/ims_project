package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.models.ExperimentDTO
import com.imsproject.gameserver.dataAccess.models.ExperimentFeedbackDTO
import com.imsproject.gameserver.dataAccess.models.ExperimentWithParticipantNamesDTO
import com.imsproject.gameserver.dataAccess.models.SessionEventDTO
import org.springframework.stereotype.Component

@Component
class ExperimentsFeedbackDAO(cursor: SQLExecutor) : DAOBase<ExperimentFeedbackDTO, ExperimentFeedbackPK>(cursor, "ExperimentsFeedback", ExperimentFeedbackPK.primaryColumnsList, arrayOf("exp_id", "pid", "question", "answer")) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): ExperimentFeedbackDTO {
        return ExperimentFeedbackDTO(
            expId = resultSet.getInt("exp_id"),
            pid = resultSet.getInt("pid"),
            question = resultSet.getString("question"),
            answer = resultSet.getString("answer")
        )
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @Throws(DaoException::class)
    override fun insert(obj: ExperimentFeedbackDTO, transactionId: String?): Int {
        val values = arrayOf<Any?>(obj.expId,obj.pid,obj.question,obj.answer)
        val idColName = "exp_id" //unnecessary
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: ExperimentFeedbackDTO, transactionId: String?) {
        throw UnsupportedOperationException("Update of experiment feedback is not supported.")
    }

    fun bulkInsert(objs: List<ExperimentFeedbackDTO>, transactionId: String? = null): Int {
        val values : List<Array<Any?>> = objs.map { arrayOf(it.expId,it.pid,it.question,it.answer) }
        return buildQueryAndBulkInsert(values, transactionId)
    }
}

data class ExperimentFeedbackPK(
    val expId: Int,
    val pid: Int,
    val question: String
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("exp_id", "pid", "question")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "exp_id" -> expId
            "pid" -> pid
            "question" -> question
            else -> null
        }
    }
}