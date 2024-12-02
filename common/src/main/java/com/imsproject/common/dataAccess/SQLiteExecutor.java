package com.imsproject.common.dataAccess;

import com.imsproject.common.dataAccess.abstracts.SQLExecutor;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.Properties;

/**
 * An implementation of {@link SQLExecutor} for SQLite databases
 */
public class SQLiteExecutor implements SQLExecutor {
    private static final String URL_PREFIX = "jdbc:sqlite:";
    private final String URL;
    private final Properties properties;
    private volatile Connection transactionConnection;

    /**
     * @param dbUrl the url of the database to connect to
     */
    public SQLiteExecutor(String dbUrl) {
        URL = URL_PREFIX + dbUrl;
        properties = getProperties();
    }

    private Properties getProperties() {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        return config.toProperties();
    }

    @Override
    public void beginTransaction() throws SQLException {
        transactionConnection = DriverManager.getConnection(URL, properties);
        transactionConnection.setAutoCommit(false);
    }

    @Override
    public void commit() throws SQLException {
        if (transactionConnection == null || transactionConnection.isClosed()){
            throw new IllegalStateException("commit() called when not in a transaction");
        }
        try{
            transactionConnection.commit();
        } finally {
            transactionConnection.close();
        }
    }

    @Override
    public void rollback() throws SQLException {

        if (transactionConnection == null || transactionConnection.isClosed()) {
            throw new IllegalStateException("rollback() called when not in a transaction");
        }
        try{
            transactionConnection.rollback();
        } finally {
            transactionConnection.close();
        }
    }

    @Override
    public OfflineResultSet executeRead(String query, Object ... params) throws SQLException {

        if(query == null || query.isBlank()){
            if(inTransaction()){
                rollback();
            }
            throw new SQLException("query is null or empty");
        }

        query = cleanQuery(query);

        if(inTransaction()){
            PreparedStatement statement = transactionConnection.prepareStatement(query);
            prepareStatement(params, statement);
            return new OfflineResultSet(statement.executeQuery());
        } else {
            try (Connection connection = DriverManager.getConnection(URL)) {
                PreparedStatement statement = connection.prepareStatement(query);
                prepareStatement(params, statement);
                ResultSet resultSet = statement.executeQuery();
                return new OfflineResultSet(resultSet);
            }
        }
    }

    @Override
    public int executeWrite(String query, Object ... params) throws SQLException {

        if(query == null || query.isBlank()){
            if(inTransaction()){
                rollback();
            }
            throw new SQLException("query is null or empty");
        }

        query = cleanQuery(query);

        if(inTransaction()){
            try{
                PreparedStatement statement = transactionConnection.prepareStatement(query);
                prepareStatement(params, statement);
                return statement.executeUpdate();
            } catch (SQLException e) {
                rollback();
                throw e;
            }
        } else {
            try (Connection connection = DriverManager.getConnection(URL, properties)) {
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(query);
                prepareStatement(params, statement);
                try{
                    int rowsChanged = statement.executeUpdate();
                    connection.commit();
                    return rowsChanged;
                }catch (SQLException e){
                    connection.rollback();
                    throw e;
                }
            }
        }
    }

    private boolean inTransaction() throws SQLException {
        return transactionConnection != null && transactionConnection.isClosed() == false;
    }

    private String cleanQuery(String query) {
        query = query.strip();
        query = query.charAt(query.length()-1) == ';' ? query : query + ";";
        return query;
    }

    private void prepareStatement(Object[] params, PreparedStatement s) throws SQLException {
        for(int i = 0; i < params.length; i++){
            s.setObject(i+1, params[i]);
        }
    }
}
