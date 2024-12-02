package com.imsproject.utils.dataAccess.abstracts;


import com.imsproject.utils.exceptions.DaoException;

public interface CounterDAO {
    Integer selectCounter() throws DaoException;

    void insertCounter(Integer value) throws DaoException;

    void incrementCounter() throws DaoException;

    void resetCounter() throws DaoException;
}
