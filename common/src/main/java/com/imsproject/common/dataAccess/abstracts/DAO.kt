package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.DaoException

/**
 * Data Access Object interface
 */
interface DAO<T, PK : PrimaryKey?> {
    @Throws(DaoException::class)
    fun select(key: PK): T

    @Throws(DaoException::class)
    fun selectAll(): List<T>

    @Throws(DaoException::class)
    fun insert(obj: T)

    @Throws(DaoException::class)
    fun insertAll(objs: List<T>)

    @Throws(DaoException::class)
    fun update(obj: T)

    @Throws(DaoException::class)
    fun updateAll(objs: List<T>)

    @Throws(DaoException::class)
    fun delete(key: PK)

    @Throws(DaoException::class)
    fun deleteAll(keys: List<PK>)

    @Throws(DaoException::class)
    fun exists(key: PK): Boolean
}
