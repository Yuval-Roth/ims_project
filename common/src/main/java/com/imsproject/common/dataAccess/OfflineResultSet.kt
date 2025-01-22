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

    inline fun <reified T> getTyped(columnName: String): T? {
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

    fun getObject(columnName: String): Any? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase()]
    }
}
