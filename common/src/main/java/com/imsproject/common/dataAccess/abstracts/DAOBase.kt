package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException
import java.util.*

abstract class DAOBase<T, PK : PrimaryKey> protected constructor(
    protected val cursor: SQLExecutor,
    protected val tableName: String
) : DAO<T, PK> {

    init {
        initTable()
    }

    /**
     * Used to automatically create a table in the database if it does not exist.
     *
     * in order to add columns and foreign keys to the table use:
     * 1. [CreateTableQueryBuilder.addColumn]
     * 2. [CreateTableQueryBuilder.addForeignKey]
     * 3. [CreateTableQueryBuilder.addCompositeForeignKey]
     */
    protected abstract fun tableQueryBuilder() : CreateTableQueryBuilder

    protected abstract fun buildObjectFromResultSet(resultSet: OfflineResultSet): T

    @Throws(DaoException::class)
    override fun select(key: PK): T {
        val values = key.values()
        val preparedQuery = StringBuilder("SELECT * FROM %s WHERE ".format(tableName))
        expandWhereClause(key, preparedQuery)

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(preparedQuery.toString(), *values)
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
        val preparedQuery = StringBuilder("DELETE FROM %s WHERE ".format(tableName))
        expandWhereClause(key, preparedQuery)

        try {
            cursor.executeWrite(preparedQuery.toString(), *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to delete from table $tableName", e)
        }
    }

    @Throws(DaoException::class)
    override fun deleteAll(keys: List<PK>) {
        try {
            cursor.beginTransaction()
        } catch (e: SQLException) {
            throw DaoException("Failed to start transaction", e)
        }

        try {
            for (key in keys) {
                delete(key)
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
    }

    @Throws(DaoException::class)
    override fun exists(key: PK): Boolean {
        val values = key.values()
        val preparedQuery = StringBuilder("SELECT * FROM %s WHERE ".format(tableName))
        expandWhereClause(key, preparedQuery)

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(preparedQuery.toString(), *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $tableName", e)
        }

        return resultSet.next()
    }

    /**
     * Upon instantiation, this method is called to create the table in the database if it does not exist.
     */
    @Throws(DaoException::class)
    private fun initTable() {
        val tableQueryBuilder = tableQueryBuilder()
        val query = tableQueryBuilder.build()
        try {
            cursor.executeWrite(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $tableName", e)
        }
    }

    private fun expandWhereClause(key: PK, preparedQuery: StringBuilder) {
        val columnNames = key.columnNames()
        for (i in columnNames.indices) {
            preparedQuery.append("%s = ?".format(columnNames[i]))
            if (i != columnNames.size - 1) {
                preparedQuery.append(" AND ")
            } else {
                preparedQuery.append(";")
            }
        }
    }
}
