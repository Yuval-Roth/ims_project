package com.imsproject.common.dataAccess.abstracts

abstract class PrimaryKeyBase (private vararg val columnNames: String) : PrimaryKey {

    private val values = mutableMapOf<String, Any>()

    /**
     * Sets the value of the column with the given name
     */
    protected fun setValue(columnName: String, value: Any) {
        if(columnName !in columnNames){
            throw IllegalArgumentException("Column name $columnName is not a primary key column")
        }
        values[columnName] = value
    }

    /**
     * Returns the values of the primary key columns
     */
    override fun columnNames(): Array<out String> {
        return columnNames
    }

    /**
     * @return the value of the column with the given name
     * if the column name does not exist, it will return null
     */
    override fun getValue(columnName: String): Any? {
        return values[columnName]
    }
}