package com.imsproject.gameserver.business.lobbies

import com.google.gson.annotations.SerializedName

enum class LobbyState {
    @SerializedName("waiting") WAITING,
    @SerializedName("playing") PLAYING
}