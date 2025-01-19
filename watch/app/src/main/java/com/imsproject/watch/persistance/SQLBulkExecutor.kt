package com.imsproject.watch.persistance

import android.database.SQLException


interface SQLBulkExecutor {

    /**
     * Executes a bulk SQL query.
     *
     * @param sql The SQL query to execute.
     * @param args The arguments to bind to the query.
     * @return The result of the query.
     */
    @Throws(SQLException::class)
    fun executeBulkInsert(sql: String, args: List<Array<out Any?>>)

}