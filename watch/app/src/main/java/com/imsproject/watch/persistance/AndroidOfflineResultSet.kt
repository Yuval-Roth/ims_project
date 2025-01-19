package com.imsproject.watch.persistance

import android.database.Cursor
import com.imsproject.common.dataAccess.OfflineResultSet

class AndroidOfflineResultSet(
    cursor: Cursor
) : OfflineResultSet {
    private val rows: List<HashMap<String, Any?>>
    val columnNames: Array<String>
    val columnCount: Int
    var currentRow: Int
        private set
    override val isEmpty: Boolean
        get() = rows.isEmpty()

    private var currentRowData: HashMap<String, Any?>? = null
    private var iterator: ListIterator<HashMap<String, Any?>>? = null

    init {
        currentRow = -1
        columnCount = cursor.columnCount
        columnNames = cursor.columnNames.map { it.lowercase() }.toTypedArray()

        rows = mutableListOf()
        while (cursor.moveToNext()) {
            val row = HashMap<String, Any?>()
            for (i in 0 until columnCount) {
                row[columnNames[i]] = cursor.getAny(i)
            }
            rows.add(row)
        }
    }

    override fun next(): Boolean {
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

    override fun getObject(columnName: String): Any? {
        val row = currentRowData ?: return null
        return row[columnName.lowercase()]
    }

    fun Cursor.getAny(index: Int): Any? {
        return when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_STRING -> getString(index)
            Cursor.FIELD_TYPE_BLOB -> getBlob(index)
            else -> throw IllegalArgumentException("Unsupported type: ${getType(index)}")
        }
    }
}