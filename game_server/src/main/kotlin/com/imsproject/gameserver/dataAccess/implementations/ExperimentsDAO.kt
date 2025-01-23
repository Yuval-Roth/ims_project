package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.models.ExperimentDTO


class ExperimentsDAO(cursor: SQLExecutor) : DAOBase<ExperimentDTO, ExperimentPK>(cursor, "Experiments", ExperimentPK.primaryColumnsList, arrayOf("pid1", "pid2")) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): ExperimentDTO {
        return ExperimentDTO(   expId = (resultSet.getObject("exp_id") as? Int),
                        pid1 = (resultSet.getObject("pid1") as? Int),
                        pid2 = (resultSet.getObject("pid2") as? Int)
        )

    }

    fun handleExperiments(action: String, exp: ExperimentDTO, transactionId: String? = null): String
    {
        when(action){
            "insert" -> {
                return Response.getOk(insert(exp, transactionId))
            }
            "delete" -> {
                if(exp.expId == null)
                    throw Exception("A lobby id was not provided for deletion")
                delete(ExperimentPK(exp.expId), transactionId)
                return Response.getOk()
            }
            "update" -> {
                if(exp.expId == null)
                    throw Exception("A lobby id was not provided for update")
                update(exp, transactionId)
                return Response.getOk()
            }
            "select" -> {
                if(exp.expId == null)
                    return Response.getOk(selectAll(transactionId))
                else
                    return Response.getOk(select(ExperimentPK(exp.expId), transactionId))
            }
            else -> throw Exception("Invalid action for participants")
        }

    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }


    @Throws(DaoException::class)
    override fun insert(obj: ExperimentDTO, transactionId: String?): Int {
        val values = arrayOf(obj.pid1,obj.pid2)
        val idColName = primaryKeyColumnNames.joinToString()
        return buildQueryAndInsert(idColName, values, transactionId)
    }

    @Throws(DaoException::class)
    override fun update(obj: ExperimentDTO, transactionId: String?) {
        val values = arrayOf(obj.pid1,obj.pid2)
        val id = obj.expId ?: throw IllegalArgumentException("Lobby ID must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, values, transactionId)
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