package com.imsproject.docker_controller

import com.google.gson.annotations.SerializedName

data class DockerProcess (

    @SerializedName("Command"      ) var Command      : String? = null,
    @SerializedName("CreatedAt"    ) var CreatedAt    : String? = null,
    @SerializedName("ID"           ) var ID           : String? = null,
    @SerializedName("Image"        ) var Image        : String? = null,
    @SerializedName("Labels"       ) var Labels       : String? = null,
    @SerializedName("LocalVolumes" ) var LocalVolumes : String? = null,
    @SerializedName("Mounts"       ) var Mounts       : String? = null,
    @SerializedName("Names"        ) var Names        : String? = null,
    @SerializedName("Networks"     ) var Networks     : String? = null,
    @SerializedName("Ports"        ) var Ports        : String? = null,
    @SerializedName("RunningFor"   ) var RunningFor   : String? = null,
    @SerializedName("Size"         ) var Size         : String? = null,
    @SerializedName("State"        ) var State        : String? = null,
    @SerializedName("Status"       ) var Status       : String? = null

)
