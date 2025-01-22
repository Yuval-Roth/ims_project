package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet

import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.models.Lobby
import java.sql.SQLException


class LobbiesDAO(cursor: SQLExecutor) : DAOBase<Lobby, LobbyPK>(cursor, "Lobbies", LobbyPK.primaryColumnsList, arrayOf("pid1", "pid2")) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Lobby {
        return Lobby(   lobbyId = (resultSet.getObject("lobby_id") as? Int),
                        pid1 = (resultSet.getObject("pid1") as? Int),
                        pid2 = (resultSet.getObject("pid2") as? Int)
        )

    }

    fun handleLobbies(action: String,lobby: Lobby): String
    {
        when(action){
            "insert" -> {
                return Response.getOk(insert(lobby))
            }
            "delete" -> {
                if(lobby.lobbyId == null)
                    throw Exception("A lobby id was not provided for deletion")
                delete(LobbyPK(lobby.lobbyId))
                return Response.getOk()
            }
            "update" -> {
                if(lobby.lobbyId == null)
                    throw Exception("A lobby id was not provided for update")
                update(lobby)
                return Response.getOk()
            }
            "select" -> {
                if(lobby.lobbyId == null)
                    return Response.getOk(selectAll())
                else
                    return Response.getOk(select(LobbyPK(lobby.lobbyId)))
            }
            else -> throw Exception("Invalid action for participants")
        }

    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        throw UnsupportedOperationException("Not yet implemented")
    }


    @Throws(DaoException::class)
    override fun insert(obj: Lobby): Int {
        val values = arrayOf(obj.pid1,obj.pid2)
        val idColName = primaryKeyColumnNames.joinToString()
        return buildQueryAndInsert(idColName, *values)
    }

    @Throws(DaoException::class)
    override fun update(obj: Lobby): Unit {
        val values = arrayOf(obj.pid1,obj.pid2)
        val id = obj.lobbyId ?: throw IllegalArgumentException("Lobby ID must not be null")
        val idColName = primaryKeyColumnNames.joinToString()
        buildQueryAndUpdate(idColName, id, *values)
    }
}


data class LobbyPK(
    val lobby_id: Int
) : PrimaryKey {
    companion object {
        val primaryColumnsList = arrayOf("lobby_id")
    }

    override fun columnNames(): Array<out String> {
        return primaryColumnsList
    }

    override fun getValue(columnName: String): Any? {
        return when (columnName) {
            "lobby_id" -> lobby_id
            else -> null
        }
    }
}