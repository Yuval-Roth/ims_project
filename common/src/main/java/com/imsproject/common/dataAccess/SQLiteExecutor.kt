package com.imsproject.common.dataAccess

import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import org.sqlite.SQLiteConfig
import java.sql.*
import java.util.*
import kotlin.concurrent.Volatile

private const val URL_PREFIX = "jdbc:sqlite:"

/**
 * An implementation of [SQLExecutor] for SQLite databases
 * @param dbUrl the path to the SQLite database file
 */
class SQLiteExecutor (dbUrl: String) : SQLExecutor {
    private val url: String = URL_PREFIX + dbUrl
    private val properties: Properties

    @Volatile
    private var transactionConnection: Connection? = null

    init {
        properties = getProperties()
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
        check(! (transactionConnection == null || transactionConnection.isClosed)) {
            "commit() called when not in a transaction"
        }
        transactionConnection.use { it.commit() }
    }

    @Throws(SQLException::class)
    override fun rollback() {
        val transactionConnection = transactionConnection
        check(! (transactionConnection == null || transactionConnection.isClosed)) {
            "rollback() called when not in a transaction"
        }
        transactionConnection.use { it.rollback() }
    }

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
            DriverManager.getConnection(url).use { connection ->
                val statement = connection.prepareStatement(cleanQuery)
                prepareStatement(params, statement)
                val resultSet = statement.executeQuery()
                return OfflineResultSet(resultSet)
            }
        }
    }

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
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [Int] representing the id of the inserted row
     */
    override fun executeInsert(query: String, vararg params: Any?): Int {
        TODO("Not yet implemented")
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

    private fun getProperties(): Properties {
        val config = SQLiteConfig()
        config.enforceForeignKeys(true)
        return config.toProperties()
    }
}
