package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException


interface SQLExecutor {
    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     * @return transaction id
     */
    @Throws(SQLException::class)
    fun beginTransaction() : String

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    @Throws(SQLException::class)
    fun commit(transactionId: String)

    /**
     * Rolls back the transaction
     * @throws SQLException if an error occurs while rolling back the transaction
     */
    @Throws(SQLException::class)
    fun rollback(transactionId: String)

    /**
     * Executes a read query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [OfflineResultSet] containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeRead(query: String, params: Array<out Any?> = emptyArray(), transactionId: String? = null): OfflineResultSet

    /**
     * Executes a generic write query by using [java.sql.PreparedStatement].
     * Can be used for either insert, update, delete
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeWrite(query: String, params: Array<out Any?> = emptyArray(), transactionId: String? = null): Int

    /**
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return A [OfflineResultSet] containing the generated keys
     */
    @Throws(SQLException::class)
    fun executeInsert(query: String, params: Array<out Any?> = emptyArray(), transactionId: String? = null): OfflineResultSet

    /**
     * Executes an update or delete query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeUpdateDelete(query: String, params: Array<out Any?> = emptyArray(), transactionId: String? = null): Int

    /**
     * Executes a bulk insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeBulkInsert(query: String, params: List<Array<out Any?>> = emptyList(), transactionId: String? = null): Int
}
