package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.models.ExperimentDTO
import com.imsproject.gameserver.dataAccess.models.ExperimentWithParticipantNamesDTO
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ExperimentsDAO(cursor: SQLExecutor) : DAOBase<ExperimentDTO, ExperimentPK>(cursor, "Experiments", ExperimentPK.primaryColumnsList, arrayOf("pid1", "pid2", "date_time")) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): ExperimentDTO {
        return ExperimentDTO(
            expId = resultSet.getInt("exp_id"),
            pid1 = resultSet.getInt("pid1"),
            pid2 = resultSet.getInt("pid2"),
            dateTime = resultSet.getLocalDateTime("date_time")
        )

    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @Throws(DaoException::class)
    override fun insert(obj: ExperimentDTO, transactionId: String?): Int {
        val values = arrayOf(obj.pid1,obj.pid2, obj.dateTime ?: LocalDateTime.now())
        val idColName = primaryKeyColumnNames.joinToString()
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: ExperimentDTO, transactionId: String?) {
        val values = arrayOf(obj.pid1,obj.pid2,obj.dateTime)
        val id = obj.expId ?: throw IllegalArgumentException("Lobby ID must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, values, transactionId)
    }

    @Throws(DaoException::class)
    fun selectWithParticipantNames(transactionId: String? = null): List<ExperimentWithParticipantNamesDTO> {
        val query = """
            SELECT e.exp_id AS eid,
                   e.date_time AS date_time,
                   p1.first_name AS p1_name,
                   p2.first_name AS p2_name
            FROM experiments e
            JOIN participants p1 ON e.pid1 = p1.pid
            JOIN participants p2 ON e.pid2 = p2.pid;
        """.trimIndent()

        return try {
            val resultSet = cursor.executeRead(query, transactionId = transactionId)
            val result = mutableListOf<ExperimentWithParticipantNamesDTO>()
            while (resultSet.next()) {
                result.add(
                    ExperimentWithParticipantNamesDTO(
                        expId = resultSet.getInt("eid")!!,
                        participant1Name = resultSet.getString("p1_name")!!,
                        participant2Name = resultSet.getString("p2_name")!!,
                        dateTime = resultSet.getLocalDateTime("date_time")!!
                    )
                )
            }
            result
        } catch (e: DaoException) {
            throw DaoException("Failed to fetch experiments with participant names", e)
        }
    }

}
data class ExperimentPK(
    val exp_id: Int
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("exp_id")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "exp_id" -> exp_id
            else -> null
        }
    }
}