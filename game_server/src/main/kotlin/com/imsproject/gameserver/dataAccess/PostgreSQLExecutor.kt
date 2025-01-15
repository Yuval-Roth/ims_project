package com.imsproject.gameserver.dataAccess

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

    @Throws(SQLException::class)
    override fun beginTransaction() {
        val connection = DriverManager.getConnection(url, properties)
        connection.autoCommit = false
        transactionConnection = connection
    }

    @Throws(SQLException::class)
    override fun commit() {
        val transactionConnection = transactionConnection
        check(!(transactionConnection == null || transactionConnection.isClosed)) {
            "commit() called when not in a transaction"
        }
        transactionConnection.use { it.commit() }
    }

    @Throws(SQLException::class)
    override fun rollback() {
        val transactionConnection = transactionConnection
        check(!(transactionConnection == null || transactionConnection.isClosed)) {
            "rollback() called when not in a transaction"
        }
        transactionConnection.use { it.rollback() }
    }

    @Throws(SQLException::class)
    override fun executeRead(query: String, vararg params: Any): OfflineResultSet {
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

    @Throws(SQLException::class)
    override fun executeWrite(query: String, vararg params: Any): Int {
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
    private fun prepareStatement(params: Array<out Any>, s: PreparedStatement) {
        for (i in params.indices) {
            s.setObject(i + 1, params[i])
        }
    }
}
