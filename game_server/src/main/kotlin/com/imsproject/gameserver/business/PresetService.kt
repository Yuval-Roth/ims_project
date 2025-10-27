package com.imsproject.gameserver.business

import com.imsproject.gameserver.dataAccess.implementations.PresetPK
import com.imsproject.gameserver.dataAccess.implementations.PresetsDAO
import com.imsproject.gameserver.dataAccess.models.PresetDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PresetService(
    val dao: PresetsDAO
) {
    fun getPresets(): List<PresetDTO> {
        return dao.selectAll()
    }

    fun addPreset(presetName: String) {
        log.debug("Adding new preset: $presetName")
        if(dao.exists(PresetPK(presetName, 0))){
            log.error("Error adding new preset: $presetName already exists")
            throw IllegalArgumentException("Preset with name $presetName already exists.")
        }
        val obj = PresetDTO(
            name = presetName,
            sessions = emptyList()
        )
        dao.insert(obj)
        log.debug("Preset added $presetName")
    }

    fun updatePreset(preset: PresetDTO) {
        log.debug("Updating preset: ${preset.name}")
        if(! dao.exists(PresetPK(preset.name, -1))){
            log.error("Error updating preset: preset ${preset.name} doesn't exist.")
            throw IllegalArgumentException("Preset with name ${preset.name} doesn't exist.")
        }
        dao.update(preset)
        val configurationLogMessage = if(preset.sessions == null || preset.sessions.isEmpty()) {
            "No sessions found"
        } else {
            preset.sessions.joinToString("\n\t")
        }
        log.debug("Preset updated: ${preset.name}\n\tnew preset configuration:\n\t$configurationLogMessage")
    }

    fun deletePreset(presetName: String) {
        log.debug("Deleting preset: $presetName")
        if(! dao.exists(PresetPK(presetName, 0))){
            log.error("Error deleting preset: $presetName doesn't exist.")
            throw IllegalArgumentException("Preset with name $presetName doesn't exist.")
        }
        val key = PresetPK(presetName,-1) // index is irrelevant for deletion of entire preset
        dao.delete(key)
        log.debug("Preset removed $presetName")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(PresetService::class.java)
    }
}