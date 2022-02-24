package com.mina_magid.magmaandroidtask.models

import com.google.gson.annotations.SerializedName

class Location {
    @SerializedName("lat")
    var latitude: Double? = null

    @SerializedName("lng")
    var longitude: Double? = null
}