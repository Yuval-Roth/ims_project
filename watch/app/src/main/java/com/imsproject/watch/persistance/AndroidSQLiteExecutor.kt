package com.imsproject.watch.persistance

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.SQLExecutor

class AndroidSQLiteExecutor(
    private val db: SQLiteDatabase
): SQLExecutor, SQLBulkExecutor by AndroidSQLiteBulkExecutor(db) {

    init{
        db.execPerConnectionSQL("PRAGMA foreign_keys = ON;", emptyArray())
    }

    @Throws(java.sql.SQLException::class)
    override fun beginTransaction() {
        try{
            db.beginTransaction()
        } catch(e: android.database.SQLException){
            throw java.sql.SQLException(e)
        }
    }

    @Throws(java.sql.SQLException::class)
    override fun commit() {
        try{
            db.setTransactionSuccessful()
        } catch(e: android.database.SQLException){
            throw java.sql.SQLException(e)
        } finally {
            db.endTransaction()
        }
    }

    @Throws(java.sql.SQLException::class)
    override fun executeRead(query: String, vararg params: Any?): OfflineResultSet {
        if (query.split(" ").first().lowercase() != "select") {
            throw IllegalArgumentException("Query must be a select query")
        }

        try {
            val cursor = db.rawQuery(query, params.map { it.toString() }.toTypedArray())
            return AndroidOfflineResultSet(cursor)
        } catch (e: android.database.SQLException) {
            throw java.sql.SQLException(e)
        }
    }

    @Throws(java.sql.SQLException::class)
    override fun executeWrite(query: String, vararg params: Any?): Int {
        val queryType = query.trim().split(" ").first().lowercase()

        val result: Int
        try{
            db.beginTransaction()
            result = when(queryType) {
                "insert" -> executeInsert(query, params)
                "update", "delete" -> executeUpdateDelete(query, params)
                else -> throw IllegalArgumentException("Unsupported query type: $queryType")
            }
            db.setTransactionSuccessful()
        } catch(e: android.database.SQLException){
            throw java.sql.SQLException(e)
        } finally {
            db.endTransaction()
        }
        return result
    }

    @Throws(java.sql.SQLException::class)
    override fun rollback() {
        try{
            db.endTransaction()
        } catch(e: android.database.SQLException){
            throw java.sql.SQLException(e)
        }
    }

    private fun executeUpdateDelete(query: String, params: Array<out Any?>) : Int {
        val statement = db.compileStatement(query)
        bindArgs(statement, params)
        return statement.executeUpdateDelete()
    }

    private fun executeInsert(query: String, params: Array<out Any?>) : Int {
        val statement = db.compileStatement(query)
        bindArgs(statement, params)
        return if(statement.executeInsert() != -1L) 1 else 0
    }

    private fun bindArgs(
        statement: SQLiteStatement,
        array: Array<out Any?>
    ) {
        array.forEachIndexed { index, value ->
            when (value) {
                null -> statement.bindNull(index + 1)
                is String -> statement.bindString(index + 1, value)
                is Int -> statement.bindLong(index + 1, value.toLong())
                is Long -> statement.bindLong(index + 1, value)
                is Float -> statement.bindDouble(index + 1, value.toDouble())
                is Double -> statement.bindDouble(index + 1, value)
                is ByteArray -> statement.bindBlob(index + 1, value)
                else -> throw IllegalArgumentException("Unsupported type: ${value.javaClass}")
            }
        }
    }
}