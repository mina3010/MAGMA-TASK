package com.mina_magid.magmaandroidtask.models

import com.google.gson.annotations.SerializedName
import java.util.*
import kotlin.collections.ArrayList

class PlaceList{
    @SerializedName("next_page_token")
    var nextPageToken: String? = ""
    @SerializedName("results")
    var places: ArrayList<SinglePlace> = ArrayList()
}