package com.imsproject.common.dataAccess

import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class OfflineResultSet(rs: ResultSet) {
    private val rows: LinkedList<HashMap<String?, Any?>>
    val columnNames: Array<String?>
    val columnCount: Int
    var currentRow: Int
        private set
    val isEmpty: Boolean
        get() = rows.isEmpty()

    init {
        currentRow = -1
        val metaData = rs.metaData
        columnCount = metaData.columnCount
        columnNames = arrayOfNulls(columnCount)
        for (i in 0 until columnCount) {
            columnNames[i] = metaData.getColumnName(i + 1).lowercase()
        }

        rows = LinkedList()
        while (rs.next()) {
            val row = HashMap<String?, Any?>()
            for (i in 0 until columnCount) {
                row[columnNames[i]] = rs.getObject(i + 1)
            }
            rows.add(row)
        }
    }
    
    private var currentRowData: HashMap<String?, Any?>? = null
    private val iterator: ListIterator<HashMap<String?, Any?>> by lazy{ rows.listIterator() }

    fun next(): Boolean {
        if (iterator.hasNext()) {
            currentRow++
            currentRowData = iterator.next()
            return true
        }
        return false
    }

    fun getObject(columnName: String): Any? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase()]
    }

    fun getString(columnName: String): String? {
        return getObject(columnName) as String?
    }

    fun getInt(columnName: String): Int? {
        return getObject(columnName) as Int?
    }

    fun getLong(columnName: String): Long? {
        return getObject(columnName) as Long?
    }

    fun getDouble(columnName: String): Double? {
        return getObject(columnName) as Double?
    }

    fun getBoolean(columnName: String): Boolean? {
        return getObject(columnName) as Boolean?
    }

    fun getLocalDate(columnName: String): LocalDate? {
        val obj = getObject(columnName) ?: return null
        return LocalDate.parse(obj as String, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun getTime(columnName: String): LocalTime? {
        val obj = getObject(columnName) ?: return null
        return LocalTime.parse(obj as String, DateTimeFormatter.ISO_LOCAL_TIME)
    }

    fun getLocalDateTime(columnName: String): LocalDateTime? {
        val obj = getObject(columnName) ?: return null
        return when (obj) {
            is LocalDateTime -> obj
            is java.sql.Timestamp -> obj.toLocalDateTime()
            is String -> LocalDateTime.parse(obj, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            else -> throw IllegalArgumentException("Unsupported type for LocalDateTime: ${obj::class.java.name}")
        }
    }

    inline fun <reified T> getEnum(columnName: String): T? {
        val obj = getObject(columnName) ?: return null
        val enumConstants = T::class.java.enumConstants
        for (enumConstant in enumConstants) {
            if((enumConstant as Enum<*>).name == obj)
                return enumConstant
        }
        throw IllegalArgumentException("Value '$obj' is not a valid enum constant for ${T::class.java.name}")
    }
}
