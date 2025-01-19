package com.imsproject.watch.persistance

import com.imsproject.common.dataAccess.abstracts.ExampleBase
import com.imsproject.common.dataAccess.abstracts.PrimaryKey

class SessionEventPrimaryKey(
    sessionId: String,
    type: String,
    subtype: String,
    timestamp: Long,
    actor: String
): ExampleBase(*columnNames()), PrimaryKey {

    companion object {
        fun columnNames() = arrayOf("sessionId", "type", "subtype", "timestamp", "actor")
    }

    init{
        setValue("sessionId", sessionId)
        setValue("type", type)
        setValue("subtype", subtype)
        setValue("timestamp", timestamp)
        setValue("actor", actor)
    }
}