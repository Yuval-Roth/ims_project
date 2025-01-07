package com.imsproject.common.dataAccess.abstracts

/**
 * An `Example` is a set of field names and values used to identify an entity
 * or a set of entities sharing the same fields and values.
 *
 * This can be used to make aggregate queries on a table.
 */
interface Example {
    fun columnNames(): Array<out String>

    fun getValue(columnName: String): Any?
}
