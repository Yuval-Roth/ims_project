package com.imsproject.common.dataAccess.abstracts;


import com.imsproject.common.dataAccess.OfflineResultSet;

import java.sql.SQLException;

public interface SQLExecutor {

    /**
     * Begins a transaction
     * @throws SQLException if an error occurs while starting the transaction
     */
    void beginTransaction() throws SQLException;

    /**
     * Commits the transaction
     * @throws SQLException if an error occurs while committing the transaction
     */
    void commit() throws SQLException;

    /**
     * Rolls back the transaction
     * @throws SQLException if an error occurs while rolling back the transaction
     */
    void rollback() throws SQLException;

    /**
     * Executes a read query by using {@link java.sql.PreparedStatement}
     * @param query An sql un-prepared query. Use '?' for parameters
     * @param params The parameters to be inserted into the query in order (will replace '?' in the query)
     * @return An {@link OfflineResultSet} containing the result of the query
     * @throws SQLException if an error occurs while executing the query
     */
    OfflineResultSet executeRead(String query, Object ... params) throws SQLException;

    /**
     * Executes a write query by using {@link java.sql.PreparedStatement}
     * @param query An sql un-prepared query. Use '?' for parameters
     * @param params The parameters to be inserted into the query in order (will replace '?' in the query)
     * @return The number of rows affected by the query
     * @throws SQLException if an error occurs while executing the query
     */
    int executeWrite(String query, Object ... params) throws SQLException;
}
