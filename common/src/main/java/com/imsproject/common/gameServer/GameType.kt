package com.imsproject.common.gameServer

import com.google.gson.annotations.SerializedName

enum class GameType {
    @SerializedName("poc") POC,
    @SerializedName("water_ripples") WATER_RIPPLES
}