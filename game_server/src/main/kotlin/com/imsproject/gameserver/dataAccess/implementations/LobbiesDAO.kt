//package com.imsproject.gameserver.dataAccess.implementations//package com.imsproject.gameserver.dataAccess.implementations
//import com.imsproject.common.dataAccess.CreateTableQueryBuilder
//import com.imsproject.common.dataAccess.OfflineResultSet
//import com.imsproject.common.dataAccess.abstracts.DAOBase
//import com.imsproject.common.dataAccess.abstracts.PrimaryKey
//import com.imsproject.common.dataAccess.abstracts.SQLExecutor
//import com.imsproject.gameserver.dataAccess.models.Lobby
//
//
//class LobbiesDAO(cursor: SQLExecutor) : DAOBase<Lobby, PrimaryKey>(cursor, "Lobbies", arrayOf("lobby_id")) {
//    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
//        val builder = CreateTableQueryBuilder("Lobbies")
//        builder.addColumn("pid1", "INT")
//        builder.addColumn("pid2", "INT")
//        builder.addForeignKey("pid1", "Participants(pid)")
//        builder.addForeignKey("pid2", "Participants(pid)")
//        return builder
//    }
//
//    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): Lobby {
//        val lobbyId = resultSet.getInt("lobby_id")
//        val pid1 = resultSet.getInt("pid1")
//        val pid2 = resultSet.getInt("pid2")
//
//        return Lobby(lobbyId, pid1, pid2)
//    }
//}
