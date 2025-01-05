package com.imsproject.common.dataAccess.abstracts

interface PrimaryKey {
    fun columnNames(): Array<out String>

    fun getValue(columnName: String): Any?
}

inline fun <reified T> PrimaryKey.getTypedValue(columnName: String): T? {
    return getValue(columnName) as? T
}

fun PrimaryKey.values(): Array<Any> {
    return columnNames().map {
        getValue(it) ?: throw IllegalStateException("Primary key value for column $it is null")
    }.toTypedArray()
}
