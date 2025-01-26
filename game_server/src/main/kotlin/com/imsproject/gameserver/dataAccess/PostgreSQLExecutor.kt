package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import java.sql.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * An implementation of [SQLExecutor] for PostgreSQL databases
 * @param dbHost the host of the PostgreSQL database (e.g., ims-db-server)
 * @param dbPort the port of the PostgreSQL database (default: 5432)
 * @param dbName the name of the PostgreSQL database
 * @param user the username for authentication
 * @param password the password for authentication
 */
class PostgreSQLExecutor(
    private val url: String,
    user: String,
    password: String
) : SQLExecutor {
    private val properties: Properties = Properties().apply {
        put("user", user)
        put("password", password)
    }

    private var transactionIdToConnection = ConcurrentHashMap<String,Connection>()

    // ==================================================================== |
    // ====================== PUBLIC METHODS ============================== |
    // ==================================================================== |

    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     */
    @Throws(SQLException::class)
    override fun beginTransaction() : String {
        val connection = DriverManager.getConnection(url, properties)
        connection.autoCommit = false
        val transactionId = UUID.randomUUID().toString()
        transactionIdToConnection[transactionId] = connection
        return transactionId
    }

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    @Throws(SQLException::class)
    override fun commit(transactionId: String) {
        val connection = transactionIdToConnection[transactionId]
        check((connection != null && ! connection.isClosed)) {
            "commit() called when not in a transaction"
        }
        connection.use { it.commit() }
        transactionIdToConnection.remove(transactionId)
    }

    /**
     * Rolls back the transaction
     * @throws SQLException if an error occurs while rolling back the transaction
     */
    @Throws(SQLException::class)
    override fun rollback(transactionId: String) {
        val transactionConnection = transactionIdToConnection[transactionId]
        check((transactionConnection != null && ! transactionConnection.isClosed)) {
            "rollback() called when not in a transaction"
        }
        transactionConnection.use { it.rollback() }
        transactionIdToConnection.remove(transactionId)
    }

    /**
     * Executes a read query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [OfflineResultSet] containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    override fun executeRead(query: String, params: Array<out Any?>, transactionId: String?): OfflineResultSet {
        requireConnection(transactionId) {
            return executeRead(it, query, params,transactionId)
        }
    }

    /**
     * Executes a generic write query by using [java.sql.PreparedStatement].
     * Can be used for either insert, update, delete
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    override fun executeWrite(query: String, params: Array<out Any?>, transactionId: String?): Int {
        requireConnection(transactionId) {
            return executeWrite(it, query, params,transactionId)
        }
    }

    /**
     * Executes an update or delete query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    override fun executeUpdateDelete(query: String, params: Array<out Any?>, transactionId: String?): Int {
        requireConnection(transactionId) {
            return executeWrite(it, query, params,transactionId)
        }
    }

    /**
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [Int] representing the id of the inserted row
     */
    override fun executeInsert(query: String, params: Array<out Any?>, transactionId: String?): OfflineResultSet {
        requireConnection(transactionId) {
            return executeInsert(it, query, params,transactionId)
        }
    }

    override fun executeBulkInsert(query: String, params: List<Array<out Any?>>, transactionId: String?): Int {
        requireConnection(transactionId) {
            return executeBulkInsert(it,query, params, transactionId)
        }
    }

    // ==================================================================== |
    // ====================== PRIVATE METHODS ============================= |
    // ==================================================================== |

    private fun executeRead(
        connection: Connection,
        query: String,
        params: Array<out Any?>,
        transactionId: String?
    ): OfflineResultSet {
        try{
            val statement = connection.prepareStatement(query.trim())
            bindParams(statement, params)
            return OfflineResultSet(statement.executeQuery())
        } finally{
            if(! inTransaction(transactionId)){
                connection.close()
            }
        }
    }

    private fun executeWrite(
        connection: Connection,
        query: String,
        params: Array<out Any?>,
        transactionId: String?
    ): Int {
        try {
            val statement = connection.prepareStatement(query.trim())
            bindParams(statement, params)
            val rowsUpdated = statement.executeUpdate()
            if(! inTransaction(transactionId)){
                connection.commit()
            }
            return rowsUpdated
        } catch(e: SQLException){
            try{
                connection.rollback()
            } catch(e2: Exception){
                e.addSuppressed(e2)
            }
            transactionId?.run{ transactionIdToConnection.remove(this) }
            connection.close()
            throw e
        } finally{
            if(! inTransaction(transactionId)){
                connection.close()
            }
        }
    }

    private fun executeInsert(
        connection: Connection,
        query: String,
        params: Array<out Any?>,
        transactionId: String?
    ): OfflineResultSet {
        try {
            val statement = connection.prepareStatement(query.trim(), Statement.RETURN_GENERATED_KEYS)
            bindParams(statement, params)
            statement.executeUpdate()
            val keysResultSet = OfflineResultSet(statement.generatedKeys)
            if(keysResultSet.isEmpty){
                throw SQLException("Inserting row failed, no ID obtained.")
            }
            if(! inTransaction(transactionId)){
                connection.commit()
            }
            return keysResultSet
        } catch(e: SQLException){
            try{
                connection.rollback()
            } catch(e2: Exception){
                e.addSuppressed(e2)
            }
            transactionId?.run{ transactionIdToConnection.remove(this) }
            connection.close()
            throw e
        } finally{
            if(! inTransaction(transactionId)){
                connection.close()
            }
        }
    }

    private fun executeBulkInsert(
        connection: Connection,
        query: String,
        paramsList: List<Array<out Any?>>,
        transactionId: String?
    ): Int {
        try {
            val statement = connection.prepareStatement(query.trim())
            for (params in paramsList) {
                bindParams(statement, params)
                statement.addBatch()
            }
            val rowsAffected = statement.executeBatch().sum()
            if (!inTransaction(transactionId)) {
                connection.commit()
            }
            return rowsAffected
        } catch (e: SQLException) {
            try {
                connection.rollback()
            } catch (e2: Exception) {
                e.addSuppressed(e2)
            }
            transactionId?.run { transactionIdToConnection.remove(this) }
            connection.close()
            throw e
        } finally {
            if (!inTransaction(transactionId)) {
                connection.close()
            }
        }
    }

    @Throws(SQLException::class)
    private fun inTransaction(transactionId: String?): Boolean {
        val connection = transactionId?.let{transactionIdToConnection[it]} ?: return false
        return ! connection.isClosed
    }

    @Throws(SQLException::class)
    private fun bindParams(statement: PreparedStatement, params: Array<out Any?>) {
        for (i in params.indices) {
            if(params[i] == null){
                statement.setNull(i + 1, Types.NULL)
            } else {
                statement.setObject(i + 1, params[i])
            }
        }
    }

    private inline fun <T> requireConnection(transactionId: String?, toExecute: (connection: Connection) -> T) : T {
        val connection = if(inTransaction(transactionId)) transactionIdToConnection[transactionId]!!
        else DriverManager.getConnection(url, properties).apply{ autoCommit = false }
        return toExecute(connection)
    }
}
