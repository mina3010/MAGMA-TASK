package com.mina_magid.magmaandroidtask.models

import com.google.gson.annotations.SerializedName
import java.util.*

class DetailSinglePlace {
    class OpeningHours {
        @SerializedName("weekday_text")
        var weekday: ArrayList<String>? = null
    }

    private val id: String? = null
    private val name: String? = null
    private val icon: String? = null

    @SerializedName("photos")
    private val photos: ArrayList<Photo>? = null

    @SerializedName("place_id")
    private val placeId: String? = null

    private val vicinity: String? = null
    private val rating = 0f

    @SerializedName("formatted_address")
    var address: String? = null

    @SerializedName("formatted_phone_number")
    var phone: String? = null

    @SerializedName("opening_hours")
    var openingHours: OpeningHours? = null

    var url: String? = null
    var website: String? = null

}
