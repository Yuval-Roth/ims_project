package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException
import java.util.*

abstract class DAOBase<T, PK : PrimaryKey> protected constructor(
    protected val cursor: SQLExecutor,
    protected val TABLE_NAME: String
) : DAO<T, PK> {

    protected val createTableQueryBuilder = CreateTableQueryBuilder.create(TABLE_NAME)

    init {
        initTable()
    }

    @Throws(DaoException::class)
    protected fun initTable() {
        initializeCreateTableQueryBuilder()
        val query = createTableQueryBuilder.build()
        try {
            cursor.executeWrite(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $TABLE_NAME", e)
        }
    }

    /**
     * Used to insert data into [DAOBase.createTableQueryBuilder].
     *
     * in order to add columns and foreign keys to the table use:
     * 1. [CreateTableQueryBuilder.addColumn]
     * 2. [CreateTableQueryBuilder.addForeignKey]<br></br><br></br>
     * 3. [CreateTableQueryBuilder.addCompositeForeignKey]
     */
    protected abstract fun initializeCreateTableQueryBuilder()

    protected abstract fun getObjectFromResultSet(resultSet: OfflineResultSet): T

    @Throws(DaoException::class)
    override fun select(key: PK): T {
        val values = keyToValues(key)
        val unpreparedQuery = StringBuilder("SELECT * FROM %s WHERE ".formatted(TABLE_NAME))
        expandWhereClause(key, unpreparedQuery)

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(unpreparedQuery.toString(), *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to select from table $TABLE_NAME", e)
        }

        if (resultSet.next()) {
            return getObjectFromResultSet(resultSet)
        } else {
            throw DaoException("Failed to select from table $TABLE_NAME")
        }
    }

    @Throws(DaoException::class)
    override fun selectAll(): List<T> {
        val query = "SELECT * FROM %s;".formatted(TABLE_NAME)
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query)
        } catch (e: SQLException) {
            throw DaoException("Failed to select all from table $TABLE_NAME", e)
        }
        val objects: MutableList<T> = LinkedList()
        while (resultSet.next()) {
            objects.add(getObjectFromResultSet(resultSet))
        }
        return objects
    }

    @Throws(DaoException::class)
    override fun delete(key: PK) {
        val values = keyToValues(key)
        val unpreparedQuery = StringBuilder("DELETE FROM %s WHERE ".formatted(TABLE_NAME))
        expandWhereClause(key, unpreparedQuery)

        try {
            cursor.executeWrite(unpreparedQuery.toString(), *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to delete from table $TABLE_NAME", e)
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
            } catch (ignored: SQLException) { }
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
        val values = keyToValues(key)
        val unpreparedQuery = StringBuilder("SELECT * FROM %s WHERE ".formatted(TABLE_NAME))
        expandWhereClause(key, unpreparedQuery)

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(unpreparedQuery.toString(), *values)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $TABLE_NAME", e)
        }

        return resultSet.next()
    }

    private fun expandWhereClause(key: PK, unpreparedQuery: StringBuilder) {
        val columnNames = key.columnNames()
        for (i in columnNames.indices) {
            unpreparedQuery.append("%s = ?".formatted(columnNames[i]))
            if (i != columnNames.size - 1) {
                unpreparedQuery.append(" AND ")
            } else {
                unpreparedQuery.append(";")
            }
        }
    }

    private fun keyToValues(key: PK): Array<Any> {
        return key.columnNames().map {
            key.getValue(it) ?: throw DaoException("Primary key value for column $it is null")
        }.toTypedArray()
    }
}
