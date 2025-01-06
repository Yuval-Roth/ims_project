package com.imsproject.common.dataAccess.abstracts

interface PrimaryKey {
    fun columnNames(): Array<out String>

    fun getValue(columnName: String): Any?

    fun values(): Array<Any> {
        return columnNames().map {
            getValue(it) ?: throw IllegalStateException("Primary key value for column $it is null")
        }.toTypedArray()
    }
}
