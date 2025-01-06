package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException
import java.util.*

abstract class DAOBase<T, PK : PrimaryKey>
/**
 * @param exampleKey an example `PrimaryKey` to build the `WHERE` clause in the SQL queries.
 * The values it contains are irrelevant, only the column names are used.
 */
protected constructor(
    protected val cursor: SQLExecutor,
    protected val tableName: String,
    exampleKey: PK
) : DAO<T, PK> {

    private val whereClause = buildWhereClause(exampleKey)
    private val deleteQuery = "DELETE FROM %s %s;".format(tableName, whereClause)
    private val selectQuery = "SELECT * FROM %s %s;".format(tableName, whereClause)

    /**
     * Used to automatically create a table in the database if it does not exist.
     *
     * in order to add columns and foreign keys to the table use:
     * 1. [CreateTableQueryBuilder.addColumn]
     * 2. [CreateTableQueryBuilder.addForeignKey]
     * 3. [CreateTableQueryBuilder.addCompositeForeignKey]
     */
    protected abstract fun getCreateTableQueryBuilder() : CreateTableQueryBuilder

    /**
     * Build a single object from a single row in the database.
     */
    protected abstract fun buildObjectFromResultSet(resultSet: OfflineResultSet): T

    /**
     * Initializes the table in the database.
     */
    @Throws(DaoException::class)
    protected fun initTable() {
        val tableQueryBuilder = getCreateTableQueryBuilder()
        val query = tableQueryBuilder.build()
        try {
            cursor.executeWrite(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $tableName", e)
        }
    }

    @Throws(DaoException::class)
    override fun select(key: PK): T {
        val values = key.values()
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(selectQuery, *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to select from table $tableName", e)
        }

        if (resultSet.next()) {
            return buildObjectFromResultSet(resultSet)
        } else {
            throw DaoException("Failed to select from table $tableName")
        }
    }

    @Throws(DaoException::class)
    override fun selectAll(): List<T> {
        val query = "SELECT * FROM %s;".format(tableName)
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query)
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
    override fun delete(key: PK) {
        val values = key.values()
        try {
            cursor.executeWrite(deleteQuery, *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to delete from table $tableName", e)
        }
    }

    @Throws(DaoException::class)
    override fun exists(key: PK): Boolean {
        val values = key.values()
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(selectQuery, *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $tableName", e)
        }

        return resultSet.next()
    }

    override fun selectAll(keys: List<PK>): List<T> {
        return doForAll(keys) { key -> select(key) }
    }

    @Throws(DaoException::class)
    override fun updateAll(objs: List<T>) {
        doForAll(objs) { obj -> update(obj) }
    }

    @Throws(DaoException::class)
    override fun insertAll(objs: List<T>) {
        doForAll(objs) { obj -> insert(obj) }
    }

    @Throws(DaoException::class)
    override fun deleteAll(keys: List<PK>) {
        doForAll(keys) { key -> delete(key) }
    }

    private inline fun <I,O> doForAll(objs: List<I>, action: (I) -> O) : List<O> {
        try {
            cursor.beginTransaction()
        } catch (e: SQLException) {
            throw DaoException("Failed to start transaction", e)
        }

        val output = mutableListOf<O>()
        try {
            for (obj in objs) {
                output.add(action(obj))
            }
        } catch (e: DaoException) {
            try {
                cursor.rollback()
            } catch (e2: SQLException) {
                e.addSuppressed(e2)
            }
            throw e
        }

        try {
            cursor.commit()
        } catch (e: SQLException) {
            throw DaoException("Failed to commit transaction", e)
        }
        return output
    }

    private fun buildWhereClause(key: PK) {
        val builder = StringBuilder("WHERE ")
        val columnNames = key.columnNames()
        for (i in columnNames.indices) {
            builder.append("%s = ?".format(columnNames[i]))
            if (i != columnNames.size - 1) {
                builder.append(" AND ")
            }
        }
    }
}
