package com.imsproject.common.dataAccess

@Suppress("ClassName")
class CreateTableQueryBuilder private constructor(private val tableName: String) {

    private val tableColumns: ArrayList<Column> = ArrayList()
    private val primaryKeys: ArrayList<String> = ArrayList()
    private val foreignKeys: ArrayList<ForeignKey> = ArrayList()
    private val checks: ArrayList<String> = ArrayList()
    private val query: StringBuilder = StringBuilder()

    enum class ColumnType {
        INTEGER,
        TEXT,
        REAL,
        BLOB,
        NUMERIC
    }

    enum class ColumnModifier {
        NOT_NULL,
        UNIQUE,
        PRIMARY_KEY,
        AUTO_INCREMENT;

        override fun toString(): String {
            return super.toString().replace("_", " ")
        }
    }

    enum class ON_DELETE {
        CASCADE,
        SET_NULL,
        SET_DEFAULT,
        RESTRICT,
        NO_ACTION;

        override fun toString(): String {
            return "ON DELETE " + super.toString().replace("_", " ")
        }
    }

    enum class ON_UPDATE {
        CASCADE,
        SET_NULL,
        SET_DEFAULT,
        RESTRICT,
        NO_ACTION;

        override fun toString(): String {
            return "ON UPDATE " + super.toString().replace("_", " ")
        }
    }

    fun addColumn(
        columnName: String,
        type: ColumnType,
        defaultValue: String? = null,
        vararg modifiers: ColumnModifier
    ) = apply {
        val isPrimaryKey = modifiers.contains(ColumnModifier.PRIMARY_KEY)
        if (isPrimaryKey) {
            primaryKeys.add(columnName)
        }

        val filteredModifiers = modifiers
            .distinct()
            .filter{ it != ColumnModifier.PRIMARY_KEY }
            .toTypedArray()

        tableColumns.add(Column(columnName, type, defaultValue, filteredModifiers))
        return this
    }

    /**
     * This method adds a composite foreign key
     *
     * @param columnNames      the columns' names in the current table
     * @param parentTableName the name of the parent table
     * @param referenceColumns the columns' names in the parent table
     * @param onDelete       the action to be taken when the parent column is deleted
     * @param onUpdate       the action to be taken when the parent column is updated
     */
    fun addCompositeForeignKey(
        columnNames: Array<String>,
        parentTableName: String,
        referenceColumns: Array<String>,
        onUpdate: ON_UPDATE? = null,
        onDelete: ON_DELETE? = null
    ) = apply {
        foreignKeys.add(ForeignKey(
            parentTableName,
            columnNames,
            referenceColumns,
            onDelete,
            onUpdate
        ))
    }

    /**
     * This method adds a non-composite foreign key
     *
     * @param columnName      the column name in the current table
     * @param parentTableName the name of the parent table
     * @param referenceColumn the column name in the parent table
     * @param onUpdate      the action to be taken when the parent column is deleted
     * @param onDelete       the action to be taken when the parent column is updated
     */
    fun addForeignKey(
        columnName: String,
        parentTableName: String,
        referenceColumn: String,
        onUpdate: ON_UPDATE? = null,
        onDelete: ON_DELETE? = null
    ) = apply {
        foreignKeys.add(ForeignKey(
            parentTableName,
            arrayOf(columnName),
            arrayOf(referenceColumn),
            onDelete,
            onUpdate
        ))
    }

    /**
     * @param check predicate
     */
    fun addCheck(check: String) = apply { checks.add(check) }

    fun build(): String {
        query.append(String.format("CREATE TABLE IF NOT EXISTS %s (\n", tableName))
        buildAllColumns()
        buildPrimaryKeys()
        buildForeignKeys()
        buildChecks()
        query.delete(query.length - 2, query.length - 1) // remove the last comma
        query.append(");")
        return query.toString()
    }

    private fun buildAllColumns() {
        for ((name, type, defaultValue, modifiers) in tableColumns) {
            query.append(String.format("\"%s\" %s", name, type))
            for (modifier in modifiers) {
                query.append(String.format(" %s", modifier))
            }
            if (! defaultValue.isNullOrEmpty()) {
                query.append(String.format(" DEFAULT %s", defaultValue))
            }
            query.append(",\n")
        }
    }

    private fun buildForeignKeys() {
        for (fk in foreignKeys) {
            query.append(
                String.format(
                    "CONSTRAINT FK_%s FOREIGN KEY (%s) REFERENCES \"%s\"(%s)",
                    generateForeignKeyName(fk),
                    columnsArrayToString(fk.columns),
                    fk.parentTableName,
                    columnsArrayToString(fk.references)
                )
            )
            if (fk.onDelete != null) {
                query.append(" ").append(fk.onDelete)
            }
            if (fk.onUpdate != null) {
                query.append(" ").append(fk.onUpdate)
            }
            query.append(",\n")
        }
    }

    private fun generateForeignKeyName(fk: ForeignKey): String {
        val name = StringBuilder()
        name.append(fk.parentTableName)
        for (columnName in fk.columns) {
            name.append("_")
            name.append(tableColumns.indexOf(Column(columnName)) + 1)
        }
        return name.toString()
    }

    private fun buildPrimaryKeys() {
        if (primaryKeys.isNotEmpty()) {
            query.append(
                String.format(
                    "PRIMARY KEY(%s),\n",
                    columnsArrayToString(primaryKeys.toTypedArray<String>())
                )
            )
        }
    }

    private fun buildChecks() {
        var i = 1
        for (check in checks) {
            query.append(String.format("CONSTRAINT CHK_%d CHECK (%s),\n", i++, check))
        }
    }

    private fun columnsArrayToString(columns: Array<String>): String {
        val columnsString = StringBuilder()
        for (i in columns.indices) {
            columnsString.append(String.format("\"%s\"", columns[i]))
            if (i != columns.size - 1) {
                columnsString.append(",")
            }
        }
        return columnsString.toString()
    }

    private data class Column(
        val name: String,
        val type: ColumnType = ColumnType.TEXT,
        val defaultValue: String? = null,
        val modifiers: Array<ColumnModifier> = emptyArray()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Column

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    private data class ForeignKey(
        val parentTableName: String,
        val columns: Array<String>,
        val references: Array<String>,
        val onDelete: ON_DELETE?,
        val onUpdate: ON_UPDATE?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ForeignKey

            if (parentTableName != other.parentTableName) return false
            if (!columns.contentEquals(other.columns)) return false
            if (!references.contentEquals(other.references)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = parentTableName.hashCode()
            result = 31 * result + columns.contentHashCode()
            result = 31 * result + references.contentHashCode()
            return result
        }
    }

    companion object {
        fun create(tableName: String): CreateTableQueryBuilder {
            return CreateTableQueryBuilder(tableName)
        }
    }
}
