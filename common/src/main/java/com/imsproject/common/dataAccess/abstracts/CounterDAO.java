package com.imsproject.common.dataAccess.abstracts;


import com.imsproject.common.dataAccess.DaoException;

public interface CounterDAO {

    Integer selectCounter() throws DaoException;

    void insertCounter(Integer value) throws DaoException;

    void incrementCounter() throws DaoException;

    void resetCounter() throws DaoException;
}
