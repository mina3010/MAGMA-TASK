package com.mina_magid.magmaandroidtask.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import java.util.*

class SinglePlace {
    var id: String? = null
    var name: String? = null
    var icon: String? = null

    @SerializedName("photos")
    var photos: ArrayList<Photo>? = null

    @SerializedName("place_id")
    var placeId: String? = null

    var vicinity: String? = null
    var rating = 0f

    @SerializedName("geometry")
    var geometry: Geometry? = null

    var loc: LatLng? =null
    var distance = 0
    var distanceString: String? = null
    var timeMinutes = 0
    var timeString: String? = null




}