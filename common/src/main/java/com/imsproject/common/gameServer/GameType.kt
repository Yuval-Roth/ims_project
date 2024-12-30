package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("poc") POC,
    @SerializedName("water_ripples") WATER_RIPPLES,
    @SerializedName("wine_glasses") WINE_GLASSES,
    @SerializedName("flour_mill") FLOUR_MILL;

    fun prettyName() = when(this) {
        POC -> "Proof of Concept"
        WATER_RIPPLES -> "Water Ripples"
        WINE_GLASSES -> "Wine Glasses"
        FLOUR_MILL -> "Flour Mill"
    }
}