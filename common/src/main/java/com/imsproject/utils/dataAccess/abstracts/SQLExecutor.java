package com.imsproject.utils.dataAccess.abstracts;


import com.imsproject.utils.dataAccess.OfflineResultSet;

import java.sql.SQLException;

public interface SQLExecutor {
    void beginTransaction() throws SQLException;
    void commit() throws SQLException;
    void rollback() throws SQLException;
    OfflineResultSet executeRead(String query) throws SQLException;
    int executeWrite(String query) throws SQLException;
}
