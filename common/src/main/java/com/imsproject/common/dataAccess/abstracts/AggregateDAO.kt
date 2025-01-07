package com.imsproject.common.dataAccess.abstracts

import com.imsproject.common.dataAccess.DaoException

/**
 * Data Access Object interface for aggregate queries
 */
interface AggregateDAO<T,EXAMPLE: Example> {
    @Throws(DaoException::class)
    fun selectAggregate(example: EXAMPLE): List<T>

    @Throws(DaoException::class)
    fun exists(example: EXAMPLE): Boolean
}
