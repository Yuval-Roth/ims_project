package com.imsproject.utils.dataAccess.abstracts;


import com.imsproject.utils.dataAccess.Cache;
import com.imsproject.utils.dataAccess.CreateTableQueryBuilder;
import com.imsproject.utils.dataAccess.OfflineResultSet;
import com.imsproject.utils.exceptions.DaoException;

import java.sql.SQLException;

public abstract class DAOBase<T, PK extends PrimaryKey> implements DAO<T, PK> {

    protected final SQLExecutor cursor;
    protected final String TABLE_NAME;
    protected final Cache<T> cache;
    protected CreateTableQueryBuilder createTableQueryBuilder;

    protected DAOBase(SQLExecutor cursor, String tableName) throws DaoException {
        this.cursor = cursor;
        this.TABLE_NAME = tableName;
        this.cache = new Cache<>();
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

    protected abstract PK getObjectFromResultSet(OfflineResultSet resultSet);

    /**
     * used for testing
     */
    public void clearTable() {
        String query = String.format("DELETE FROM %s", TABLE_NAME);
        try {
            cursor.executeWrite(query);
            clearCache();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearCache() {
        cache.clear();
    }
}
