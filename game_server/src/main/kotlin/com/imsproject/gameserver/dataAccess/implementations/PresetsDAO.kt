package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.ExampleBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.dataAccess.models.PresetDTO
import com.imsproject.gameserver.dataAccess.models.PresetSessionDTO
import org.springframework.stereotype.Service

@Service
class PresetsDAO(cursor: SQLExecutor): DAOBase<PresetDTO, PresetPK>(
    cursor,
    "presets",
    arrayOf("preset_name,index"),
    arrayOf(
        "duration",
        "game_type",
        "sync_tolerance",
        "sync_window_length",
        "is_warmup"
    )
) {
    override fun buildObjectFromResultSet(resultSet: OfflineResultSet): PresetDTO {
        val presetSessionDTOs = mutableListOf<PresetSessionDTO>()
        val presetName = resultSet.getString("preset_name")!!
        do {
            val session = PresetSessionDTO(
                index = resultSet.getInt("index")!!,
                duration = resultSet.getInt("duration")!!,
                gameType = GameType.valueOf(resultSet.getString("game_type")!!),
                syncTolerance = resultSet.getInt("sync_tolerance")!!,
                syncWindowLength = resultSet.getInt("sync_window_length")!!,
                isWarmup = resultSet.getBoolean("is_warmup")!!
            )
            presetSessionDTOs.add(session)
        } while (resultSet.next())
        return PresetDTO(
            name = presetName,
            sessions = presetSessionDTOs
        )
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        TODO("Not yet implemented")
    }

    override fun insert(
        obj: PresetDTO,
        transactionId: String?
    ): Int {
        var rowsInserted = 0
        for (session in obj.sessions) {
            val query = "INSERT INTO presets (preset_name, index, duration, game_type, sync_tolerance, sync_window_length, is_warmup) VALUES (?,?,?,?,?,?,?)"
            val values = arrayOf<Any>(
                obj.name,
                session.index,
                session.duration,
                session.gameType.name,
                session.syncTolerance,
                session.syncWindowLength,
                session.isWarmup
            )
            rowsInserted += cursor.executeWrite(query,values, transactionId)
        }
        return rowsInserted
    }

    override fun update(obj: PresetDTO, transactionId: String?) {
        throw NotImplementedError("Update not implemented for PresetsDAO")
    }

    override fun delete(key: PresetPK, transactionId: String?) {
        val query = "DELETE FROM presets WHERE preset_name = ?"
        val values = arrayOf(key.getValue("preset_name")!!)
        cursor.executeWrite(query, values, transactionId)
    }
}

class PresetPK(
    presetName: String,
    index: Int
): ExampleBase("preset_name","index"), PrimaryKey {
    init {
        setValue("preset_name", presetName)
        setValue("index", index)
    }

}