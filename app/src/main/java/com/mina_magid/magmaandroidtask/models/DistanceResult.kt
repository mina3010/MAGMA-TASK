package com.mina_magid.magmaandroidtask.models

import com.google.gson.annotations.SerializedName
import java.util.*

class DistanceResult {
    var status: String? = null

    @SerializedName("origin_addresses")
    var originAddresses: ArrayList<String>? = null

    @SerializedName("destination_addresses")
    var destinationAddresses: ArrayList<String>? = null

    var rows: ArrayList<ElementsArray>? = null
}