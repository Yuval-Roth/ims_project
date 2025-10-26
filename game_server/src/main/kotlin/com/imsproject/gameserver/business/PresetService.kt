package com.imsproject.gameserver.business

import com.imsproject.common.dataAccess.DaoException
import com.imsproject.gameserver.dataAccess.implementations.PresetPK
import com.imsproject.gameserver.dataAccess.implementations.PresetsDAO
import com.imsproject.gameserver.dataAccess.models.PresetDTO
import org.springframework.stereotype.Service

@Service
class PresetService(
    val dao: PresetsDAO
) {
    fun getPresets(): List<PresetDTO> {
        return dao.selectAll()
    }

    fun addPreset(presetName: String) {
        if(dao.exists(PresetPK(presetName, 0))){
            throw IllegalArgumentException("Preset with name $presetName already exists.")
        }
        val obj = PresetDTO(
            name = presetName,
            sessions = emptyList()
        )
        dao.insert(obj)
    }

    fun updatePreset(preset: PresetDTO) {
        if(! dao.exists(PresetPK(preset.name, -1))){
            throw IllegalArgumentException("Preset with name ${preset.name} doesn't exist.")
        }
        dao.update(preset)
    }

    fun deletePreset(presetName: String) {
        if(! dao.exists(PresetPK(presetName, 0))){
            throw IllegalArgumentException("Preset with name $presetName doesn't exist.")
        }
        val key = PresetPK(presetName,-1) // index is irrelevant for deletion of entire preset
        dao.delete(key)
    }
}