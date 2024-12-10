package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("poc") POC,
    @SerializedName("water_ripples") WATER_RIPPLES;

    fun prettyName() = when(this) {
        POC -> "Proof of Concept"
        WATER_RIPPLES -> "Water Ripples"
    }
}