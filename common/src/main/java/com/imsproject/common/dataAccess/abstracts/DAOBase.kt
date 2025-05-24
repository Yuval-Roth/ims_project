package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException
import java.util.*

abstract class DAOBase<T, PK : PrimaryKey> protected constructor(
    protected val cursor: SQLExecutor,
    protected val tableName: String,
    protected val primaryKeyColumnNames : Array<out String>,
    protected val nonKeyColumnNames : Array<out String>
) : DAO<T, PK> {

    private val deleteQuery: String
    private val selectQuery: String

    init{
        val whereClause = buildWhereClause(primaryKeyColumnNames)
        deleteQuery = "DELETE FROM $tableName $whereClause;"
        selectQuery = "SELECT * FROM $tableName $whereClause;"
    }

    /**
     * Used to automatically create a table in the database if it does not exist.
     *
     * in order to add columns, foreign keys and checks to the table use:
     * 1. [CreateTableQueryBuilder.addColumn]
     * 2. [CreateTableQueryBuilder.addForeignKey]
     * 3. [CreateTableQueryBuilder.addCompositeForeignKey]
     * 4. [CreateTableQueryBuilder.addCheck]
     */
    protected abstract fun getCreateTableQueryBuilder() : CreateTableQueryBuilder

    /**
     * Build a single object from a single row in the database.
     */
    protected abstract fun buildObjectFromResultSet(resultSet: OfflineResultSet): T

    /**
     * Initializes the table in the database if it does not exist.
     */
    @Throws(DaoException::class)
    protected fun initTable(transactionId: String?) {
        val tableQueryBuilder = getCreateTableQueryBuilder()
        val query = tableQueryBuilder.build()
        try {
            cursor.executeWrite(query, transactionId = transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to initialize table $tableName", e)
        }
    }

    @Throws(DaoException::class)
    override fun select(key: PK, transactionId: String?): T {
        val values = key.values()
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(selectQuery, values, transactionId = transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to select from table $tableName", e)
        }

        if (resultSet.next()) {
            return buildObjectFromResultSet(resultSet)
        } else {
            throw DaoException("Requested item might not exist in table $tableName")
        }
    }

    @Throws(DaoException::class)
    override fun selectAggregate(columns : Array<String>, values : Array<out Any>, transactionId: String?): List<T> {
        val query = "SELECT * FROM $tableName ${buildWhereClause(columns)};"

        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query,values, transactionId = transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to select aggregate from table $tableName", e)
        }
        val objects: MutableList<T> = LinkedList()
        while (resultSet.next()) {
            objects.add(buildObjectFromResultSet(resultSet))
        }
        return objects

    }

    @Throws(DaoException::class)
    override fun selectAll(transactionId: String?): List<T> {
        val query = "SELECT * FROM $tableName;"
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(query, transactionId = transactionId)
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
    override fun delete(key: PK, transactionId: String?) {
        val values = key.values()
        try {
            cursor.executeWrite(deleteQuery, values, transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to delete from table $tableName", e)
        }
    }

    @Throws(DaoException::class)
    override fun exists(key: PK, transactionId: String?): Boolean {
        val values = key.values()
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(selectQuery,  values, transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $tableName", e)
        }

        return resultSet.next()
    }

    override fun selectAll(keys: List<PK>, transactionId: String?): List<T> {
        return doForAll(keys) { key -> select(key) }
    }

    @Throws(DaoException::class)
    override fun updateAll(objs: List<T>, transactionId: String?) {
        doForAll(objs) { obj -> update(obj) }
    }

    @Throws(DaoException::class)
    override fun insertAll(objs: List<T>, transactionId: String?): List<Int> {
        return doForAll(objs) { obj -> insert(obj) }
    }

    @Throws(DaoException::class)
    override fun deleteAll(keys: List<PK>, transactionId: String?) {
        doForAll(keys) { key -> delete(key) }
    }

    private inline fun <I,O> doForAll(objs: List<I>, action: (I) -> O) : List<O> {
        val transactionId: String
        try {
            transactionId = cursor.beginTransaction()
        } catch (e: SQLException) {
            throw DaoException("Failed to start transaction", e)
        }

        val output = mutableListOf<O>()
        for (obj in objs) {
            output.add(action(obj))
        }

        try {
            cursor.commit(transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to commit transaction", e)
        }
        return output
    }

    @Suppress("DuplicatedCode")
    private fun buildWhereClause(columnNames: Array<out String>): String {
        val builder = StringBuilder("WHERE ")
        for (i in columnNames.indices) {
            builder.append("${columnNames[i]} = ?")
            if (i != columnNames.size - 1) {
                builder.append(" AND ")
            }
        }
        return builder.toString()
    }

    fun buildQueryAndInsert(idColumnName: String, values: Array<out Any?>, transactionId: String?): Int {
        val questionmarks = List(nonKeyColumnNames.size) { "?" }.joinToString(", "); //don't ask please
        val insertQuery = "INSERT INTO $tableName (${nonKeyColumnNames.joinToString()}) VALUES (${questionmarks}) RETURNING $idColumnName"
        try {
            val keysResultSet = cursor.executeInsert(insertQuery,values, transactionId)
            if(keysResultSet.next()) {
                val id = keysResultSet.getInt(idColumnName)
                if(id != null){
                    return id;
                }
            }
        } catch (e: SQLException) {
            throw DaoException("Failed to insert to table $tableName", e)
        }
        throw DaoException("Error in insertion to $tableName")
    }

    /**
     * @return the affected row count
     */
    fun buildQueryAndBulkInsert(values: List<Array<out Any?>>, transactionId: String?): Int {
        val questionmarks = List(nonKeyColumnNames.size) { "?" }.joinToString(", "); //don't ask please
        val insertQuery = "INSERT INTO $tableName (${nonKeyColumnNames.joinToString()}) VALUES (${questionmarks})"
        try {
            return cursor.executeBulkInsert(insertQuery,values, transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to insert to table $tableName", e)
        }
    }

    fun buildQueryAndUpdate(idColumnName: String, id: Int, values: Array<out Any?>, transactionId: String?): Unit {
        try {
            // Build the query dynamically
            val updates = mutableListOf<String>()
            val params = mutableListOf<Any?>()

            val fields = nonKeyColumnNames.zip(values).toMap()

            fields.forEach { (column, value) ->
                value?.let {
                    updates.add("$column = ?")
                    params.add(it)
                }
            }

            if (updates.isEmpty()) {
                throw IllegalArgumentException("No fields to update")
            }

            val query = "UPDATE $tableName SET ${updates.joinToString(", ")} WHERE $idColumnName = ?"
            params.add(id)

            cursor.executeWrite(query, params.toTypedArray(), transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to update table $tableName", e)
        }
    }

}
