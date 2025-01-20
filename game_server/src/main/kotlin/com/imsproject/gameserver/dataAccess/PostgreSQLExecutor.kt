package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess//package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import java.sql.*
import java.util.*
import kotlin.concurrent.Volatile

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
    private val properties: Properties

    @Volatile
    private var transactionConnection: Connection? = null

    init {
        properties = Properties().apply {
            put("user", user)
            put("password", password)
        }
    }

    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     */
    @Throws(SQLException::class)
    override fun beginTransaction() {
        val connection = DriverManager.getConnection(url, properties)
        connection.autoCommit = false
        transactionConnection = connection
    }

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    @Throws(SQLException::class)
    override fun commit() {
        val transactionConnection = transactionConnection
        check(!(transactionConnection == null || transactionConnection.isClosed)) {
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
        val transactionConnection = transactionConnection
        check(!(transactionConnection == null || transactionConnection.isClosed)) {
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
        if (query.isBlank()) {
            if (inTransaction()) {
                rollback()
            }
            throw SQLException("query is empty")
        }

        val cleanQuery = cleanQuery(query)

        if (inTransaction()) {
            val statement = transactionConnection!!.prepareStatement(cleanQuery)
            prepareStatement(params, statement)
            return OfflineResultSet(statement.executeQuery())
        } else {
            DriverManager.getConnection(url, properties).use { connection ->
                val statement = connection.prepareStatement(cleanQuery)
                prepareStatement(params, statement)
                val resultSet = statement.executeQuery()
                return OfflineResultSet(resultSet)
            }
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
        if (query.isBlank()) {
            if (inTransaction()) {
                rollback()
            }
            throw SQLException("query is empty")
        }

        val cleanQuery = cleanQuery(query)

        if (inTransaction()) {
            try {
                val statement = transactionConnection!!.prepareStatement(cleanQuery)
                prepareStatement(params, statement)
                return statement.executeUpdate()
            } catch (e: SQLException) {
                rollback()
                throw e
            }
        } else {
            DriverManager.getConnection(url, properties).use { connection ->
                connection.autoCommit = false
                val statement = connection.prepareStatement(cleanQuery)
                prepareStatement(params, statement)
                try {
                    val rowsChanged = statement.executeUpdate()
                    connection.commit()
                    return rowsChanged
                } catch (e: SQLException) {
                    connection.rollback()
                    throw e
                }
            }
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
        TODO("Not yet implemented")
    }

    /**
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [Int] representing the id of the inserted row
     */
    override fun executeInsert(query: String, vararg params: Any?): Int {
        TODO("Not yet implemented")
    }

    @Throws(SQLException::class)
    private fun inTransaction(): Boolean {
        return transactionConnection != null && transactionConnection!!.isClosed.not()
    }

    private fun cleanQuery(query: String): String {
        var output = query.trim()
        output = if (output[output.length - 1] == ';') output else "$output;"
        return output
    }

    @Throws(SQLException::class)
    private fun prepareStatement(params: Array<out Any?>, s: PreparedStatement) {
        for (i in params.indices) {
            if(params[i] == null){
                s.setNull(i + 1, Types.NULL)
            } else {
                s.setObject(i + 1, params[i])
            }
        }
    }
}
