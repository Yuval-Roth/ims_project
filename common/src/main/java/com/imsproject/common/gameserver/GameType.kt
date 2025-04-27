package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("undefined")       UNDEFINED,
    @SerializedName("water_ripples")   WATER_RIPPLES,
    @SerializedName("wine_glasses")    WINE_GLASSES,
    @SerializedName("flour_mill")      FLOUR_MILL,
    @SerializedName("flower_garden")   FLOWER_GARDEN;


    fun prettyName() = when(this) {
        UNDEFINED -> "Undefined"
        WATER_RIPPLES -> "Water Ripples"
        WINE_GLASSES -> "Wine Glasses"
        FLOUR_MILL -> "Flour Mill"
        FLOWER_GARDEN -> "Flower Garden"
    }
}