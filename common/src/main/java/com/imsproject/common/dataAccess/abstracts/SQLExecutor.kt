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
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [OfflineResultSet] containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeRead(query: String, vararg params: Any?): OfflineResultSet

    /**
     * Executes a generic write query by using [java.sql.PreparedStatement].
     * Can be used for either insert, update, delete
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeWrite(query: String, vararg params: Any?): Int

    /**
     * Executes an insert query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return An [Int] representing the id of the inserted row
     */
    @Throws(SQLException::class)
    fun executeInsert(query: String, vararg params: Any?): Int

    /**
     * Executes an update or delete query by using [java.sql.PreparedStatement]
     * @param query A sql prepared query. Use '?' for parameters
     * @param params The parameters to be used in the query in order
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    @Throws(SQLException::class)
    fun executeUpdateDelete(query: String, vararg params: Any?): Int
}
