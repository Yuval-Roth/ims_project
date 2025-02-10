package com.imsproject.servermanager

import com.google.gson.annotations.SerializedName

data class DockerProcess (

    @SerializedName("Command"      ) var command      : String? = null,
    @SerializedName("CreatedAt"    ) var createdAt    : String? = null,
    @SerializedName("ID"           ) var id           : String? = null,
    @SerializedName("Image"        ) var image        : String? = null,
    @SerializedName("Labels"       ) var labels       : String? = null,
    @SerializedName("LocalVolumes" ) var localVolumes : String? = null,
    @SerializedName("Mounts"       ) var mounts       : String? = null,
    @SerializedName("Names"        ) var names        : String? = null,
    @SerializedName("Networks"     ) var networks     : String? = null,
    @SerializedName("Ports"        ) var ports        : String? = null,
    @SerializedName("RunningFor"   ) var runningFor   : String? = null,
    @SerializedName("Size"         ) var size         : String? = null,
    @SerializedName("State"        ) var state        : String? = null,
    @SerializedName("Status"       ) var status       : String? = null

)
