package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException
import java.util.*

/**
 * Base class for aggregate DAOs.
 * Inheriting classes are meant to be used as delegates
 * for doing aggregate queries in existing tables.
 */
abstract class AggregateDAOBase<T, EXAMPLE : Example> protected constructor(
    protected val cursor: SQLExecutor,
    protected val tableName: String,
) : AggregateDAO<T, EXAMPLE> {

    /**
     * Build a single object from a single row in the database.
     */
    protected abstract fun buildObjectFromResultSet(resultSet: OfflineResultSet): T

    @Throws(DaoException::class)
    override fun selectAggregate(example: EXAMPLE): List<T> {
        val (columns, values) = extractFieldsFromExample(example)
        val query = "SELECT * FROM $tableName ${buildWhereClause(columns)};"

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query,*values)
        } catch (e: SQLException) {
            throw DaoException("Failed to select all from table $tableName", e)
        }
        val objects: MutableList<T> = LinkedList()
        while (resultSet.next()) {
            objects.add(buildObjectFromResultSet(resultSet))
        }
        return objects

    }

    @Throws(DaoException::class)
    override fun exists(example: EXAMPLE): Boolean {
        val (columns, values) = extractFieldsFromExample(example)
        val query = "SELECT * FROM $tableName ${buildWhereClause(columns)};"

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query, *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $tableName", e)
        }

        return resultSet.next()
    }

    private fun extractFieldsFromExample(example: EXAMPLE): Pair<Array<String>, Array<Any>> {
        val columns = example.columnNames().filter { example.getValue(it) != null }.toTypedArray()
        val values = columns.map { example.getValue(it)!! }.toTypedArray()
        return columns to values
    }

    @Suppress("DuplicatedCode")
    private fun buildWhereClause(columnNames: Array<out String>) {
        val builder = StringBuilder("WHERE ")
        for (i in columnNames.indices) {
            builder.append("${columnNames[i]} = ?")
            if (i != columnNames.size - 1) {
                builder.append(" AND ")
            }
        }
    }
}