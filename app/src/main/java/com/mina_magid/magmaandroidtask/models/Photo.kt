package com.mina_magid.magmaandroidtask.models

import com.google.gson.annotations.SerializedName

class Photo {
    var height = 0
    var width:Int = 0

    @SerializedName("photo_reference")
    var photoReference: String? = null
}