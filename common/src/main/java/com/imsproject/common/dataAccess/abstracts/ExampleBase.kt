package com.imsproject.common.dataAccess.abstracts

abstract class ExampleBase (private vararg val columnNames: String) : Example {

    private val values = mutableMapOf<String, Any>()

    /**
     * Sets the value of the column with the given name
     */
    protected fun setValue(columnName: String, value: Any) {
        if(columnName !in columnNames){
            throw IllegalArgumentException("$columnName is not a column in the object")
        }
        values[columnName] = value
    }

    /**
     * @return the names of the columns in the object
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