package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("undefined")       UNDEFINED,
    @SerializedName("water_ripples")   WATER_RIPPLES,
    @SerializedName("wine_glasses")    WINE_GLASSES,
    @SerializedName("flour_mill")      FLOUR_MILL,
    @SerializedName("flower_garden")   FLOWER_GARDEN,
    @SerializedName("waves")           WAVES,
    @SerializedName("particles")       PARTICLES
    ;


    fun prettyName() = when(this) {
        UNDEFINED -> "Undefined"
        WATER_RIPPLES -> "Water Ripples"
        WINE_GLASSES -> "Wine Glasses"
        FLOUR_MILL -> "Flour Mill"
        FLOWER_GARDEN -> "Flower Garden"
        WAVES -> "Waves"
        PARTICLES -> "Particles"
    }

    fun hebrewName() = when(this) {
        UNDEFINED -> "לא מוגדר"
        WATER_RIPPLES -> "אדוות מים"
        WINE_GLASSES -> "כוסות יין"
        FLOUR_MILL -> "מטחנת קמח"
        FLOWER_GARDEN -> "גינת פרחים"
        WAVES -> "גלים"
        PARTICLES -> "חלקיקים"
    }
}