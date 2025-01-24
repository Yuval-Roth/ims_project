package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.DaoException

/**
 * Data Access Object interface
 */
interface DAO<T, PK : PrimaryKey> {
    @Throws(DaoException::class)
    fun select(key: PK, transactionId : String? = null): T

    @Throws(DaoException::class)
    fun selectAll(transactionId : String? = null): List<T>

    @Throws(DaoException::class)
    fun selectAll(keys: List<PK>, transactionId : String? = null): List<T>

    @Throws(DaoException::class)
    fun insert(obj: T, transactionId : String? = null) : Int

    @Throws(DaoException::class)
    fun insertAll(objs: List<T>, transactionId : String? = null): List<Int>

    @Throws(DaoException::class)
    fun update(obj: T, transactionId : String? = null)

    @Throws(DaoException::class)
    fun updateAll(objs: List<T>, transactionId : String? = null)

    @Throws(DaoException::class)
    fun delete(key: PK, transactionId : String? = null)

    @Throws(DaoException::class)
    fun deleteAll(keys: List<PK>, transactionId : String? = null)

    @Throws(DaoException::class)
    fun exists(key: PK, transactionId : String? = null): Boolean
}
