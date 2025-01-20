package com.imsproject.common.dataAccess.abstracts

/**
 * A primary key is an [Example] for an entity whose values are used to uniquely identify it
 * and those values cannot be null. this can only be used to make single entity queries (not aggregate queries)
 *
 * implementing this interface is mostly semantic, but provides a default method [values]
 * that checks that all the values are not null and returns them as an array
 */
interface PrimaryKey : Example {
    fun values(): Array<out Any> {
        return columnNames().map {
            getValue(it) ?: throw IllegalStateException("Primary key value for column $it is null")
        }.toTypedArray()
    }
}
