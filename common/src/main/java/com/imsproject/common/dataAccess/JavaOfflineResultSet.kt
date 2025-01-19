package com.imsproject.common.dataAccess

import java.sql.ResultSet
import java.util.*

class JavaOfflineResultSet(rs: ResultSet) : OfflineResultSet {
    private val rows: LinkedList<HashMap<String?, Any>>
    val columnNames: Array<String?>
    val columnCount: Int
    var currentRow: Int
        private set
    override val isEmpty: Boolean
        get() = rows.isEmpty()

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
        return row[columnName.lowercase(Locale.getDefault())]
    }
}
