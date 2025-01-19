@file:JvmName("JavaOfflineResultSetKt")

package com.imsproject.common.dataAccess

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

interface OfflineResultSet {
    val isEmpty: Boolean
    fun next(): Boolean
    fun getObject(columnName: String): Any?
}

inline fun <reified T> OfflineResultSet.getTyped(columnName: String): T? {
    val obj = getObject(columnName) ?: return null
    if(T::class == LocalDateTime::class)
        return LocalDateTime.parse(obj as String, DateTimeFormatter.ISO_LOCAL_DATE_TIME) as T
    if(T::class == LocalDate::class)
        return LocalDate.parse(obj as String, DateTimeFormatter.ISO_LOCAL_DATE) as T
    if(T::class == LocalTime::class)
        return LocalTime.parse(obj as String, DateTimeFormatter.ISO_LOCAL_TIME) as T
    if(T::class.java.isEnum){
        val enumConstants = T::class.java.enumConstants
        for (enumConstant in enumConstants) {
            if((enumConstant as Enum<*>).name == obj)
                return enumConstant
        }
    }

    return obj as T
}

