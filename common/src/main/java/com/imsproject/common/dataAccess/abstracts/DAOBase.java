package com.imsproject.common.dataAccess.abstracts;

import com.imsproject.common.dataAccess.CreateTableQueryBuilder;
import com.imsproject.common.dataAccess.DaoException;
import com.imsproject.common.dataAccess.OfflineResultSet;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class DAOBase<T, PK extends PrimaryKey> implements DAO<T, PK> {

    protected final SQLExecutor cursor;
    protected final String TABLE_NAME;
    protected CreateTableQueryBuilder createTableQueryBuilder;

    protected DAOBase(SQLExecutor cursor, String tableName) throws DaoException {
        this.cursor = cursor;
        this.TABLE_NAME = tableName;
        this.createTableQueryBuilder = CreateTableQueryBuilder.create(tableName);
        initTable();
    }

    protected void initTable() throws DaoException {
        initializeCreateTableQueryBuilder();
        String query = createTableQueryBuilder.build();
        try {
            cursor.executeWrite(query);
        } catch (SQLException e) {
            throw new DaoException("Failed to initialize table "+TABLE_NAME, e);
        }
    }

    /**
     * Used to insert data into {@link DAOBase#createTableQueryBuilder}. <br/>
     * in order to add columns and foreign keys to the table use:<br/><br/>
     * {@link CreateTableQueryBuilder#addColumn(String, CreateTableQueryBuilder.ColumnType, CreateTableQueryBuilder.ColumnModifier...)} <br/><br/>
     * {@link CreateTableQueryBuilder#addForeignKey(String, String, String)}<br/><br/>
     * {@link CreateTableQueryBuilder#addCompositeForeignKey(String[], String, String[])}
     */
    protected abstract void initializeCreateTableQueryBuilder();

    protected abstract T getObjectFromResultSet(OfflineResultSet resultSet);

    @Override
    public T select(PK key) throws DaoException {
        Object[] values = keyToValues(key);
        StringBuilder unpreparedQuery = new StringBuilder("SELECT * FROM %s WHERE ".formatted(TABLE_NAME));
        expandWhereClause(key, unpreparedQuery);

        OfflineResultSet resultSet;
        try {
            resultSet = cursor.executeRead(unpreparedQuery.toString(), values);
        } catch (SQLException e) {
            throw new DaoException("Failed to select from table "+TABLE_NAME, e);
        }

        if (resultSet.next()) {
            return getObjectFromResultSet(resultSet);
        } else {
            throw new DaoException("Failed to select from table "+TABLE_NAME);
        }
    }

    @Override
    public List<T> selectAll() throws DaoException {
        String query = "SELECT * FROM %s;".formatted(TABLE_NAME);
        OfflineResultSet resultSet;
        try {
            resultSet = cursor.executeRead(query);
        } catch (SQLException e) {
            throw new DaoException("Failed to select all from table "+TABLE_NAME, e);
        }
        List<T> objects = new LinkedList<>();
        while (resultSet.next()) {
            objects.add(getObjectFromResultSet(resultSet));
        }
        return objects;
    }

    @Override
    public void delete(PK key) throws DaoException {
        Object[] values = keyToValues(key);
        StringBuilder unpreparedQuery = new StringBuilder("DELETE FROM %s WHERE ".formatted(TABLE_NAME));
        expandWhereClause(key, unpreparedQuery);

        try {
            cursor.executeWrite(unpreparedQuery.toString(), values);
        } catch (SQLException e) {
            throw new DaoException("Failed to delete from table "+TABLE_NAME, e);
        }
    }

    @Override
    public void deleteAll(List<PK> keys) throws DaoException {
        try {
            cursor.beginTransaction();
        } catch (SQLException e) {
            throw new DaoException("Failed to start transaction", e);
        }

        try{
            for (PK key : keys) {
                delete(key);
            }
        } catch (DaoException e) {
            try {
                cursor.rollback();
            } catch (SQLException ignored) {}
            throw e;
        }

        try {
            cursor.commit();
        } catch (SQLException e) {
            throw new DaoException("Failed to commit transaction", e);
        }
    }

    @Override
    public boolean exists(PK key) throws DaoException {
        Object[] values = keyToValues(key);
        StringBuilder unpreparedQuery = new StringBuilder("SELECT * FROM %s WHERE ".formatted(TABLE_NAME));
        expandWhereClause(key, unpreparedQuery);

        OfflineResultSet resultSet;
        try {
            resultSet = cursor.executeRead(unpreparedQuery.toString(), values);
        } catch (SQLException e) {
            throw new DaoException("Failed to check if exists in table "+TABLE_NAME, e);
        }

        return resultSet.next();
    }

    protected void expandWhereClause(PK key, StringBuilder unpreparedQuery) {
        String[] columnNames = key.columnNames();
        for (int i = 0; i < columnNames.length; i++) {
            unpreparedQuery.append("%s = ?".formatted(columnNames[i]));
            if (i != columnNames.length - 1) {
                unpreparedQuery.append(" AND ");
            } else {
                unpreparedQuery.append(";");
            }
        }
    }

    protected Object[] keyToValues(PK key) {
        return Arrays.stream(key.columnNames()).map(key::getValue).toArray();
    }
}
