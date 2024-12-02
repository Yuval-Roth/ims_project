package com.imsproject.utils.dataAccess.abstracts;

import com.imsproject.utils.exceptions.DaoException;

import java.util.List;

/**
 * Data Access Object interface
 * @param <T> the type of the object
 * @param <PK> the type of the primary key
 */
public interface DAO<T, PK extends PrimaryKey> {

    T select(PK key) throws DaoException;

    List<T> selectAll() throws DaoException;

    void insert(T object) throws DaoException;

    void insertAll(List<T> objects) throws DaoException;

    void update(T object) throws DaoException;

    void updateAll(List<T> objects) throws DaoException;

    void delete(PK object) throws DaoException;

    void deleteAll(List<PK> objects) throws DaoException;

    boolean exists(PK object) throws DaoException;
}
