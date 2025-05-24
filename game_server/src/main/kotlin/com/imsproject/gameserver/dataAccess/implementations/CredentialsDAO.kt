package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.ExampleBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.gameserver.business.auth.Credentials
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class CredentialsDAO(
    cursor: SQLExecutor
) : DAOBase<Credentials, CredentialsPK>(cursor, "Credentials", arrayOf("user_id"), arrayOf("password")) {

    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Credentials {
        return Credentials(
            userId = resultSet.getString("user_id")!!,
            password = resultSet.getString("password")!!
        )
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @Throws(DaoException::class)
    override fun insert(obj: Credentials, transactionId: String?): Int {
        val query = "INSERT INTO Credentials (user_id, password) VALUES (?, ?)"
        val values = arrayOf<Any>(obj.userId, obj.password)
        try{
            return cursor.executeWrite(query, values, transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to insert credentials for user ${obj.userId}", e)
        }
    }

    @Throws(DaoException::class)
    override fun update(obj: Credentials, transactionId: String?) {
        throw UnsupportedOperationException("Update of credentials is not supported.")
    }
}
class CredentialsPK(
    val userId: String
) : ExampleBase("user_id"), PrimaryKey {
    init {
        setValue("user_id", userId)
    }
}

