package com.mina_magid.magmaandroidtask.remote

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.mina_magid.magmaandroidtask.R

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class GooglePlacesApi (ctx: Context){
    val SEARCH_RADIUS = 5000

    val TYPE_RESTAURANT = 0

    val RANKBY_PROMINENCE = 0
    val RANKBY_DISTANCE = 1

    var ctx: Context? = null

    fun getRestaurantListClient(): ServiceAPI? {
        val BASE_URL = "https://maps.googleapis.com/maps/api/"

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ServiceAPI::class.java)
    }

    fun getTypeString(type: Int): String? {
        when (type) {
            TYPE_RESTAURANT -> return "Restaurant"
        }
        return ""
    }

    fun getType(s: String?): Int {
        when (s) {
            "Restaurant" -> return TYPE_RESTAURANT
        }
        return TYPE_RESTAURANT
    }

    fun getRank(s: String?): Int {
        when (s) {
            "Prominence" -> return RANKBY_PROMINENCE
            "Distance" -> return RANKBY_DISTANCE
        }
        return RANKBY_PROMINENCE
    }


    fun getQueryParams(loc: LatLng, type: Int, rankby: Int): HashMap<String, String> {
        val params = HashMap<String, String>()
        params["key"] ="AIzaSyAiAir1uMz3NwJDd9vjIhqeEuTUgw2S7VM"
        val latlng = loc.latitude.toString() + "," + loc.longitude
        params["location"] = latlng
        when (type) {
            TYPE_RESTAURANT -> params["type"] = "restaurant"
        }
        when (rankby) {
            RANKBY_DISTANCE -> params["rankby"] = "distance"
            RANKBY_PROMINENCE -> params["radius"] = SEARCH_RADIUS.toString()
        }
        return params
    }
}