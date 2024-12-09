package com.imsproject.common.dataAccess

import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class OfflineResultSet(rs: ResultSet) {
    private val rows: LinkedList<HashMap<String?, Any>>
    val columnNames: Array<String?>
    val columnCount: Int
    var currentRow: Int
        private set
    private var currentRowData: HashMap<String?, Any>? = null
    private var iterator: ListIterator<HashMap<String?, Any>>? = null

    init {
        currentRow = -1
        val metaData = rs.metaData
        columnCount = metaData.columnCount
        columnNames = arrayOfNulls(columnCount)
        for (i in 0 until columnCount) {
            columnNames[i] = metaData.getColumnName(i + 1).lowercase(Locale.getDefault())
        }

        rows = LinkedList()
        while (rs.next()) {
            val row = HashMap<String?, Any>()
            for (i in 0 until columnCount) {
                row[columnNames[i]] = rs.getObject(i + 1)
            }
            rows.add(row)
        }
    }

    fun next(): Boolean {
        val iterator = this.iterator ?: run {
            val newIterator = rows.listIterator()
            newIterator
        }
        if (iterator.hasNext()) {
            currentRow++
            currentRowData = iterator.next()
            return true
        }
        return false
    }

    fun previous(): Boolean {
        val iterator = this.iterator ?: return false // if iterator is null, there are no previous rows
        if (iterator.hasPrevious()) {
            currentRow--
            currentRowData = iterator.previous()
            return true
        }
        return false
    }

    fun reset() {
        iterator = null
        currentRow = -1
        currentRowData = null
    }

    val isEmpty: Boolean
        get() = rows.isEmpty()

    fun getObject(columnName: String): Any? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase(Locale.getDefault())]
    }

    fun getString(columnName: String): String? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase(Locale.getDefault())] as String?
    }

    fun getInt(columnName: String): Int? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase(Locale.getDefault())] as Int?
    }

    fun getBoolean(columnName: String): Boolean? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase(Locale.getDefault())] as Boolean?
    }

    fun getDouble(columnName: String): Double? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase(Locale.getDefault())] as Double?
    }

    fun getLocalDateTime(columnName: String): LocalDateTime? {
        val rawString = getString(columnName.lowercase(Locale.getDefault())) ?: return null
        return LocalDateTime.parse(
            rawString,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
    }

    fun getLocalDate(columnName: String): LocalDate? {
        val rawString = getString(columnName.lowercase(Locale.getDefault())) ?: return null
        return LocalDate.parse(rawString, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun getLocalTime(columnName: String): LocalTime? {
        val rawString = getString(columnName.lowercase(Locale.getDefault())) ?: return null
        return LocalTime.parse(rawString, DateTimeFormatter.ISO_LOCAL_TIME)
    }
}
