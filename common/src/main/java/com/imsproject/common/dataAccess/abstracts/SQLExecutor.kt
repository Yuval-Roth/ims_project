package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.OfflineResultSet
import java.sql.SQLException


interface SQLExecutor {
    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     */
    @Throws(SQLException::class)
    fun beginTransaction()

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    @Throws(SQLException::class)
    fun commit()

    /**
     * Rolls back the transaction
     * @throws SQLException if an error occurs while rolling back the transaction
     */
    @Throws(SQLException::class)
    fun rollback()

    /**
     * Executes a read query by using [java.sql.PreparedStatement]
     * @param query An sql un-prepared query. Use '?' for parameters
     * @param params The parameters to be inserted into the query in order (will replace '?' in the query)
     * @return An [OfflineResultSet] containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeRead(query: String, vararg params: Any): OfflineResultSet

    /**
     * Executes a write query by using [java.sql.PreparedStatement]
     * @param query An sql un-prepared query. Use '?' for parameters
     * @param params The parameters to be inserted into the query in order (will replace '?' in the query)
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeWrite(query: String, vararg params: Any): Int
}
