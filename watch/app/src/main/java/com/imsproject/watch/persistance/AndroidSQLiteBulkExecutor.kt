package com.imsproject.watch.persistance

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement

class AndroidSQLiteBulkExecutor (
    private val db: SQLiteDatabase
) : SQLBulkExecutor {

    override fun executeBulkInsert(
        sql: String,
        args: List<Array<out Any?>>
    ) {
        db.beginTransaction()
        try {
            val statement = db.compileStatement(sql)
            args.forEach {
                statement.clearBindings()
                bindArgs(statement, it)
                if(statement.executeInsert() == -1L){
                    throw SQLException("Failed to insert row")
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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