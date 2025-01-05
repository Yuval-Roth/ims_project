package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.*
import java.sql.SQLException


abstract class CounterDAOBase protected constructor(
    private val cursor: SQLExecutor,
    private val tableName: String,
    private val columnName: String
) : CounterDAO {

    init {
        initTable()
    }

    @Throws(DaoException::class)
    protected fun initTable() {
        val createTableQueryBuilder = CreateTableQueryBuilder.create(tableName)

        createTableQueryBuilder.addColumn(
            columnName,
            ColumnType.INTEGER,
            modifiers = ColumnModifier().NOT_NULL
        )

        var query = createTableQueryBuilder.build()
        try {
            cursor.executeWrite(query)
            query = String.format("SELECT * FROM %s;", tableName)
            if (cursor.executeRead(query).isEmpty) {
                resetCounter()
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $tableName", e)
        }
    }

    /**
     * @return the counter's value
     * @throws DaoException if an error occurred while trying to select the value
     */
    @Throws(DaoException::class)
    override fun selectCounter(): Int {
        val query = String.format("SELECT %s FROM %s;", columnName, tableName)
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to select $columnName", e)
        }
        if (resultSet.next()) {
            return resultSet.getTyped(columnName) ?: throw DaoException("Failed to select $columnName")
        } else {
            throw DaoException("Failed to select $columnName")
        }
    }

    /**
     * @param value - new value for the counter
     * @throws DaoException if an error occurred while trying to insert the value
     */
    @Throws(DaoException::class)
    override fun insertCounter(value: Int) {
        val query = String.format(
            "DELETE FROM %s; INSERT INTO %s (%s) VALUES (%d);",
            tableName,
            tableName,
            columnName,
            value
        )
        try {
            if (cursor.executeWrite(query) == 0) {
                throw RuntimeException("Unexpected error while inserting $columnName")
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to insert $columnName", e)
        }
    }

    /**
     * @throws DaoException if an error occurred while trying to increment the counter
     */
    @Throws(DaoException::class)
    override fun incrementCounter() {
        val query = String.format(
            " UPDATE %s SET %s = (SELECT %s FROM %s) + 1;",
            tableName,
            columnName,
            columnName,
            tableName
        )
        try {
            if (cursor.executeWrite(query) != 1) {
                throw RuntimeException("Unexpected error while incrementing $columnName")
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to increment $columnName", e)
        }
    }

    /**
     * @throws DaoException if an error occurred while trying to reset the counter
     */
    @Throws(DaoException::class)
    override fun resetCounter() {
        insertCounter(1)
    }
}
