package com.imsproject.common.dataAccess.abstracts;


import com.imsproject.common.dataAccess.*;

import java.sql.SQLException;

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

    protected abstract PK getObjectFromResultSet(OfflineResultSet resultSet);

}