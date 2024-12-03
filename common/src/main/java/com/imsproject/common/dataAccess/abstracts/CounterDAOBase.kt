package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException


abstract class CounterDAOBase protected constructor(
    private val cursor: SQLExecutor,
    private val TABLE_NAME: String,
    private val COLUMN_NAME: String
) : CounterDAO {

    init {
        initTable()
    }

    @Throws(DaoException::class)
    protected fun initTable() {
        val createTableQueryBuilder = CreateTableQueryBuilder.create(TABLE_NAME)

        createTableQueryBuilder.addColumn(
            COLUMN_NAME,
            CreateTableQueryBuilder.ColumnType.INTEGER,
            CreateTableQueryBuilder.ColumnModifier.NOT_NULL
        )

        var query = createTableQueryBuilder.build()
        try {
            cursor.executeWrite(query)
            query = String.format("SELECT * FROM %s;", TABLE_NAME)
            if (cursor.executeRead(query).isEmpty) {
                resetCounter()
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $TABLE_NAME", e)
        }
    }

    /**
     * @return the counter's value
     * @throws DaoException if an error occurred while trying to select the value
     */
    @Throws(DaoException::class)
    override fun selectCounter(): Int {
        val query = String.format("SELECT %s FROM %s;", COLUMN_NAME, TABLE_NAME)
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to select $COLUMN_NAME", e)
        }
        if (resultSet.next()) {
            return resultSet.getInt(COLUMN_NAME)
        } else {
            throw DaoException("Failed to select $COLUMN_NAME")
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
            TABLE_NAME,
            TABLE_NAME,
            COLUMN_NAME,
            value
        )
        try {
            if (cursor.executeWrite(query) == 0) {
                throw RuntimeException("Unexpected error while inserting $COLUMN_NAME")
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to insert $COLUMN_NAME", e)
        }
    }

    /**
     * @throws DaoException if an error occurred while trying to increment the counter
     */
    @Throws(DaoException::class)
    override fun incrementCounter() {
        val query = String.format(
            " UPDATE %s SET %s = (SELECT %s FROM %s) + 1;",
            TABLE_NAME,
            COLUMN_NAME,
            COLUMN_NAME,
            TABLE_NAME
        )
        try {
            if (cursor.executeWrite(query) != 1) {
                throw RuntimeException("Unexpected error while incrementing $COLUMN_NAME")
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to increment $COLUMN_NAME", e)
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
