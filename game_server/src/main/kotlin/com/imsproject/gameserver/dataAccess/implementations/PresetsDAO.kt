package com.imsproject.gameserver.dataAccess.implementations

import com.imsproject.common.dataAccess.CreateTableQueryBuilder
import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.dataAccess.OfflineResultSet
import com.imsproject.common.dataAccess.abstracts.DAOBase
import com.imsproject.common.dataAccess.abstracts.ExampleBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey
import com.imsproject.common.dataAccess.abstracts.SQLExecutor
import com.imsproject.common.gameserver.GameType
import com.imsproject.gameserver.dataAccess.models.PresetDTO
import com.imsproject.gameserver.dataAccess.models.PresetSessionDTO
import org.springframework.stereotype.Component
import java.sql.SQLException
import java.util.LinkedList

@Component
class PresetsDAO(cursor: SQLExecutor): DAOBase<PresetDTO, PresetPK>(
    cursor,
    "presets",
    arrayOf("preset_name,idx"),
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
        while(resultSet.next()) { // Move to the first session row
            val session = PresetSessionDTO(
                index = resultSet.getInt("idx")!!,
                duration = resultSet.getInt("duration")!!,
                gameType = GameType.valueOf(resultSet.getString("game_type")!!),
                syncTolerance = resultSet.getInt("sync_tolerance")!!,
                syncWindowLength = resultSet.getInt("sync_window_length")!!,
                isWarmup = resultSet.getBoolean("is_warmup")!!
            )
            presetSessionDTOs.add(session)
        }
        return PresetDTO(
            name = presetName,
            sessions = presetSessionDTOs
        )
    }

    override fun getCreateTableQueryBuilder(): CreateTableQueryBuilder {
        TODO("Not yet implemented")
    }

    override fun selectAll(transactionId: String?): List<PresetDTO>{
        val allNamesQuery = """
            SELECT DISTINCT preset_name FROM presets
            ORDER BY preset_name ASC;
        """.trimIndent()
        var resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(allNamesQuery, transactionId = transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to select all from table $tableName", e)
        }
        val presetNames = mutableListOf<String>()
        while (resultSet.next()) {
            presetNames.add(resultSet.getString("preset_name")!!)
        }
        val objects: MutableList<PresetDTO> = LinkedList()
        for (presetName in presetNames) {
            val query = """
            SELECT * FROM presets
            WHERE preset_name = ?
            ORDER BY idx ASC;
        """.trimIndent()
            try {
                resultSet = cursor.executeRead(query,arrayOf(presetName),transactionId)
            } catch (e: SQLException) {
                throw DaoException("Failed to select all from table $tableName", e)
            }
            while (resultSet.next()) {
                objects.add(buildObjectFromResultSet(resultSet))
            }
        }
        return objects
    }

    override fun insert(
        obj: PresetDTO,
        transactionId: String?
    ): Int {
        var rowsInserted = 0
        try {
            val query = "INSERT INTO presets (preset_name, idx, duration, game_type, sync_tolerance, sync_window_length, is_warmup) VALUES (?,?,?,?,?,?,?)"
            // Insert a first row with index 0 to represent the preset itself
            val firstValue = arrayOf<Any>(obj.name,0,-1,GameType.UNDEFINED.name,-1,-1,false)
            val _transactionId = transactionId ?: cursor.beginTransaction()
            rowsInserted += cursor.executeWrite(query, firstValue, _transactionId)
            if (obj.sessions != null){
                for (session in obj.sessions) {
                    val values = arrayOf<Any>(
                        obj.name,
                        session.index,
                        session.duration,
                        session.gameType.name,
                        session.syncTolerance,
                        session.syncWindowLength,
                        session.isWarmup
                    )
                    rowsInserted += cursor.executeWrite(query,values, _transactionId)
                }
            }
            if(transactionId == null){
                cursor.commit(_transactionId)
            }
        } catch (e: SQLException){
            throw DaoException("Failed to insert preset ${obj.name}", e)
        }
        return rowsInserted
    }

    override fun update(obj: PresetDTO, transactionId: String?) {
        val _transactionId: String
        try{
            _transactionId = transactionId ?: cursor.beginTransaction()
        } catch (e: SQLException){
            throw DaoException("Failed to begin transaction for updating preset ${obj.name}", e)
        }

        try{
            delete(PresetPK(obj.name,-1), _transactionId)
            insert(obj, _transactionId)
            cursor.commit(_transactionId)
        } catch (e: DaoException){
            throw DaoException("Failed to update preset ${obj.name}", e)
        }
    }

    override fun delete(key: PresetPK, transactionId: String?) {
        val query = "DELETE FROM presets WHERE preset_name = ?"
        val values = arrayOf(key.getValue("preset_name")!!)
        try {
            cursor.executeWrite(query, values, transactionId)
        } catch (e: SQLException){
            throw DaoException("Failed to delete preset with name ${key.getValue("preset_name")}", e)
        }
    }

    override fun exists(key: PresetPK, transactionId: String?): Boolean {
        val selectQuery = """
            SELECT * FROM $tableName
            WHERE preset_name = ? and idx = 0
        """.trimIndent()
        val values = arrayOf(key.getValue("preset_name"))
        val resultSet: OfflineResultSet
        try {
            resultSet = cursor.executeRead(selectQuery,  values, transactionId)
        } catch (e: SQLException) {
            throw DaoException("Failed to check if exists in table $tableName", e)
        }
        return resultSet.next()
    }
}

class PresetPK(
    presetName: String,
    index: Int
): ExampleBase("preset_name","idx"), PrimaryKey {
    init {
        setValue("preset_name", presetName)
        setValue("idx", index)
    }

}