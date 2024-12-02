package com.imsproject.common.dataAccess.abstracts

interface PrimaryKey {
    fun columnNames(): Array<out String>

    fun getValue(columnName: String): Any?
}
