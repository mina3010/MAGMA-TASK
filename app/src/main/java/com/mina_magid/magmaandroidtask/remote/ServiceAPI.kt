package com.mina_magid.magmaandroidtask.remote

import com.mina_magid.magmaandroidtask.models.DetailResult
import com.mina_magid.magmaandroidtask.models.DistanceResult
import com.mina_magid.magmaandroidtask.models.PlaceList
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface ServiceAPI {
    @GET("place/nearbysearch/json")
    fun getNearbyRestaurant(@QueryMap params: Map<String, String>): Call<PlaceList?>?

    @GET("distancematrix/json")
    fun getRestaurantDistances(@QueryMap params: Map<String, String>): Call<DistanceResult?>?

    @GET("place/details/json")
    fun getRestaurantDetails(@QueryMap params: Map<String, String>): Call<DetailResult?>?
}