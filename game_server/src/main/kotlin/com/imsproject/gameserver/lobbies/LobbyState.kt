package com.imsproject.gameserver.lobbies

import com.google.gson.annotations.SerializedName

enum class LobbyState {
    @SerializedName("waiting") WAITING,
    @SerializedName("playing") PLAYING
}