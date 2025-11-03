package com.imsproject.common.gameserver

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("undefined")       UNDEFINED,
    @SerializedName("recess")          RECESS,
    @SerializedName("water_ripples")   WATER_RIPPLES,
    @SerializedName("wine_glasses")    WINE_GLASSES,
    @SerializedName("flour_mill")      FLOUR_MILL,
    @SerializedName("flower_garden")   FLOWER_GARDEN,
    @SerializedName("waves")           WAVES,
    @SerializedName("pacman")          PACMAN,
    @SerializedName("tree")            TREE
    ;


    fun prettyName() = when(this) {
        UNDEFINED -> "Undefined"
        RECESS -> "Recess"
        WATER_RIPPLES -> "Water Ripples"
        WINE_GLASSES -> "Wine Glasses"
        FLOUR_MILL -> "Flour Mill"
        FLOWER_GARDEN -> "Flower Garden"
        WAVES -> "Waves"
        PACMAN -> "Pacman"
        TREE -> "Tree"
    }

    fun hebrewName() = when(this) {
        UNDEFINED -> "לא מוגדר"
        RECESS -> "הפסקה"
        WATER_RIPPLES -> "אדוות מים"
        WINE_GLASSES -> "כוסות יין"
        FLOUR_MILL -> "מטחנת קמח"
        FLOWER_GARDEN -> "גינת פרחים"
        WAVES -> "גלים"
        PACMAN -> "פאקמן"
        TREE -> "עץ"
    }
}