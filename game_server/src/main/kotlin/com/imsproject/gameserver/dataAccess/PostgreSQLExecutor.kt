package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import java.sql.*
import java.util.*

private const val URL_PREFIX = "jdbc:postgresql://"

/**
 * An implementation of [SQLExecutor] for PostgreSQL databases
 * @param dbHost the host of the PostgreSQL database (e.g., ims-db-server)
 * @param dbPort the port of the PostgreSQL database (default: 5432)
 * @param dbName the name of the PostgreSQL database
 * @param user the username for authentication
 * @param password the password for authentication
 */
class PostgreSQLExecutor(
    dbHost: String,
    dbPort: Int = 5432,
    dbName: String,
    user: String,
    password: String
) : SQLExecutor {
    private val url: String = "$URL_PREFIX$dbHost:$dbPort/$dbName"
    private val properties: Properties = Properties().apply {
        put("user", user)
        put("password", password)
    }

    private var transactionConnection = ThreadLocal<Connection?>()

    // ==================================================================== |
    // ====================== PUBLIC METHODS ============================== |
    // ==================================================================== |

    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     */
    @Throws(SQLException::class)
    override fun beginTransaction() {
        val connection = DriverManager.getConnection(url, properties)
        connection.autoCommit = false
        transactionConnection.set(connection)
    }

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    @Throws(SQLException::class)
    override fun commit() {
        val transactionConnection = transactionConnection.get()
        check((transactionConnection != null && ! transactionConnection.isClosed)) {
            "commit() called when not in a transaction"
        }
        transactionConnection.use { it.commit() }
    }

    /**
     * Rolls back the transaction
     * @throws SQLException if an error occurs while rolling back the transaction
     */
    @Throws(SQLException::class)
    override fun rollback() {
        val transactionConnection = transactionConnection.get()
        check((transactionConnection != null && ! transactionConnection.isClosed)) {
            "rollback() called when not in a transaction"
        }
        transactionConnection.use { it.rollback() }
    }

    /**
     * Executes a read query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [OfflineResultSet] containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    override fun executeRead(query: String, vararg params: Any?): OfflineResultSet {
        requireConnection {
            return executeRead(it, query, *params)
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
    override fun executeWrite(query: String, vararg params: Any?): Int {
        requireConnection {
            return executeWrite(it, query, *params)
        }
    }

    /**
     * Executes an update or delete query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    override fun executeUpdateDelete(query: String, vararg params: Any?): Int {
        requireConnection {
            return executeWrite(it, query, *params)
        }
    }

    /**
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [Int] representing the id of the inserted row
     */
    override fun executeInsert(query: String, vararg params: Any?): OfflineResultSet {
        requireConnection {
            return executeInsert(it, query, *params)
        }
    }

    // ==================================================================== |
    // ====================== PRIVATE METHODS ============================= |
    // ==================================================================== |

    private fun executeRead(connection: Connection, query: String, vararg params: Any?): OfflineResultSet {
        try{
            val statement = connection.prepareStatement(query.trim())
            bindParams(statement, params)
            return OfflineResultSet(statement.executeQuery())
        } finally{
            if(! inTransaction()){
                connection.close()
            }
        }
    }

    private fun executeWrite(connection: Connection, query: String, vararg params: Any?): Int {
        try {
            val statement = connection.prepareStatement(query.trim())
            bindParams(statement, params)
            val rowsUpdated = statement.executeUpdate()
            if(! inTransaction()){
                connection.commit()
            }
            return rowsUpdated
        } catch(e: SQLException){
            try{
                connection.rollback()
            } catch(e2: Exception){
                e.addSuppressed(e2)
            }
            connection.close()
            throw e
        } finally{
            if(! inTransaction()){
                connection.close()
            }
        }
    }



    private fun executeInsert(connection: Connection, query: String, vararg params: Any?): OfflineResultSet {
        try {
            val statement = connection.prepareStatement(query.trim(), Statement.RETURN_GENERATED_KEYS)
            bindParams(statement, params)
            statement.executeUpdate()
            val keysResultSet = OfflineResultSet(statement.generatedKeys)
            if(keysResultSet.isEmpty){
                throw SQLException("Inserting row failed, no ID obtained.")
            }
            if(! inTransaction()){
                connection.commit()
            }
            return keysResultSet
        } catch(e: SQLException){
            try{
                connection.rollback()
            } catch(e2: Exception){
                e.addSuppressed(e2)
            }
            connection.close()
            throw e
        } finally{
            if(! inTransaction()){
                connection.close()
            }
        }
    }

    @Throws(SQLException::class)
    private fun inTransaction(): Boolean {
        val connection = transactionConnection.get() ?: return false
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

    private inline fun <T> requireConnection(toExecute: (connection: Connection) -> T) : T {
        val connection = if(inTransaction()) transactionConnection.get()!!
        else DriverManager.getConnection(url, properties).apply{ autoCommit = false }
        return toExecute(connection)
    }
}
