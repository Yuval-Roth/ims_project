package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.DaoException


interface CounterDAO {
    @Throws(DaoException::class)
    fun selectCounter(): Int

    @Throws(DaoException::class)
    fun insertCounter(value: Int)

    @Throws(DaoException::class)
    fun incrementCounter()

    @Throws(DaoException::class)
    fun resetCounter()
}
