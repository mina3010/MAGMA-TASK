package com.mina_magid.magmaandroidtask.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mina_magid.magmaandroidtask.R
import com.mina_magid.magmaandroidtask.models.*
import com.mina_magid.magmaandroidtask.remote.GooglePlacesApi
import com.mina_magid.magmaandroidtask.remote.ServiceAPI
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    var mapReady = false
    var mapAccuracy = 10000f
    private var mInterstitialAd: InterstitialAd? = null
    companion object {
        var googlePlacesApi: GooglePlacesApi? = null
        var serviceAPI: ServiceAPI? = null
        var placeList: PlaceList? = null
        var curMarker: Marker? = null
        var distanceResult: DistanceResult? = null
    }
    private var defLocation = LatLng(27.1770325, 31.2019277) //assuit
    var curLocation = defLocation
    private var locationType: Int = GooglePlacesApi(this).TYPE_RESTAURANT
    private var locationRankby: Int = GooglePlacesApi(this).RANKBY_PROMINENCE
    private var locMan: LocationManager? = null
    private var locLis: LocationListener? = null
    private var locationPermissionGranted = false
    private var PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var dbReference: DatabaseReference = database.getReference("trackMe")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        googlePlacesApi = GooglePlacesApi(this)
        googlePlacesApi?.getRestaurantListClient()
        serviceAPI = googlePlacesApi?.getRestaurantListClient()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        //search on map by text
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val location: String = search.query.toString()
                var addressList: List<android.location.Address>? = null
                if (location != null || location == "") {
                    val geocoder = Geocoder(this@MainActivity)
                    try {
                        addressList = geocoder.getFromLocationName(location, 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    if (addressList!!.isEmpty() || addressList == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "not found on map , check your text on map",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val address = addressList[0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        mMap!!.addMarker(MarkerOptions().position(latLng).title(location))
                        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        // Initialize Admob
        MobileAds.initialize(this) {}
        var adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError?.message)
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            })
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null;
            }
        }
        showFullAd()

        //button get restaurants nearby user
        find_rest_btn.setOnClickListener {
            initMapPointer(curLocation)
//            mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
            updateLocationUI()
            getLocationPermission()
        }
        // button restart used when GPS network is closed and user need restart activity
        restart.setOnClickListener {
            val intent = intent
            finish()
            startActivity(intent)
        }

        //TODO tracking me not working yet ..
        // Get a reference from the database so that the app can read and write operations
        dbReference = Firebase.database.reference
        dbReference.addValueEventListener(locListener)


    }

    //TODO tracking me not working yet ..
     val locListener = object : ValueEventListener {
        //     @SuppressLint("LongLogTag")
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                //get the exact longitude and latitude from the database "test"
                val location = snapshot.child("trackMe").getValue(LocationInfo::class.java)
                val locationLat = location?.locationLat
                val locationLong = location?.locationLong
                Log.d("TAG4","${locationLat}||$locationLong")

                //trigger reading of location from database using the button
                find_location_btn.setOnClickListener {
                    Log.d("TAG5","${locationLat}||$locationLong")

                    // check if the latitude and longitude is not null
//                    if (locationLat != null && locationLong!= null) {
                        // create a LatLng object from location
                        val latLng = LatLng(locationLat!!, locationLong!!)
                        //create a marker at the read location and display it on the map
                        mMap?.addMarker(
                            MarkerOptions().position(latLng)
                                .title("The user is currently here")
                        )
                        //specify how the map camera is updated
                        val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                        //update the camera with the CameraUpdate object
                        mMap?.moveCamera(update)
//                    }
//                    else {
                        // if location is null , log an error message
                        Log.e(TAG, "user location cannot be found")
//                    }
                }

            }
        }
        // show this toast if there is an error while reading from the database
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_LONG).show()
        }

    }

    private fun showFullAd(){
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        dummyData()
    }

    private fun dummyData() {
        val locationListNew: ArrayList<Address> = ArrayList<Address>()
        locationListNew.add(
            Address(
                "Grand Kadri Hotel By Cristal Lebanon",
                33.85148430277257,
                35.895525763213946
            )
        )
        locationListNew.add(Address("Germanos - Pastry", 33.85217073479985, 35.89477838111461))
        locationListNew.add(Address("Malak el Tawook", 33.85334017189446, 35.89438946093824))
        locationListNew.add(Address("Z Burger House", 33.85454300475094, 35.894561122304474))
        locationListNew.add(Address("Collège Oriental", 33.85129821373707, 35.89446263654391))
        locationListNew.add(Address("VERO MODA", 33.85048738635312, 35.89664059012788))

        if (locationListNew.isNotEmpty()) {
            for (i in locationListNew.indices) {
                Log.d("TAG3", "${locationListNew[i].lat!!}")
                val loc = LatLng(locationListNew[i].lat!!, locationListNew[i].lon!!)
                mMap?.addMarker(
                    MarkerOptions().position(loc).title(locationListNew[i].name!!)
                )
                mMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        loc,
                        13f
                    )
                ) // your zoom in map
            }
        }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    fun updateLocationUI() {
        locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsEnabled: Boolean = locMan!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled: Boolean = locMan!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage(resources.getString(R.string.gps_network_not_enabled))
            dialog.setPositiveButton(
                resources.getString(R.string.open_location_settings)
            ) { dialog, which ->
                val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(i)
                Toast.makeText(
                    this,
                    R.string.restart_app_after_enabling_gps,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            dialog.setNegativeButton(
                resources.getString(R.string.cancel)
            ) { dialog, which -> //Nothing to do here
                Toast.makeText(this, R.string.enable_gps, Toast.LENGTH_SHORT).show()
            }
            dialog.show()
        } else {
            //Get current location coord
            locLis = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    curLocation = LatLng(location.latitude, location.longitude)
                    mapAccuracy = location.accuracy
                    if (mMap != null) initMapPointer(curLocation)
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locMan!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100, 1f,
                locLis as LocationListener
            )
            locMan!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                100, 1f,
                locLis as LocationListener
            )
        }
    }

    fun initMapPointer(loc: LatLng?) {
        initCurrentPointer(loc!!)
        if (mapAccuracy != 10000f) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locMan!!.removeUpdates(locLis!!)
            //getHospitalLocation(curLocation)
        }
    }

    private fun initCurrentPointer(loc: LatLng) {
        mMap?.clear()
        curMarker = mMap?.addMarker(
            MarkerOptions().position(loc).title("Current Location")
                .snippet("(" + loc.latitude + "," + loc.longitude + ")")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 18f)) // your zoom in map
        mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        getRestaurantNearby(loc)

    }


    fun addMapMarker(loc: LatLng?, name: String?, vicinity: String?) {
        mMap?.addMarker(
            MarkerOptions().position(loc!!).title(name)
                .snippet(vicinity)
        )

    }

    private fun getRestaurantNearby(loc: LatLng?) {

        Log.d("TAG", "${loc?.latitude},${loc?.longitude}")
        val params: HashMap<String, String> = MainActivity.googlePlacesApi!!.getQueryParams(
            loc!!,
            locationType,
            locationRankby
        )
        Log.d("TAG", "${loc?.latitude},${loc?.longitude}")


        serviceAPI?.getNearbyRestaurant(params)!!.enqueue(object :
            retrofit2.Callback<PlaceList?> {
            override fun onResponse(
                call: Call<PlaceList?>, response: retrofit2.Response<PlaceList?>,
            ) {
                if (response.isSuccessful) {
                    MainActivity.placeList = response.body()
                    if (MainActivity.placeList != null && MainActivity.placeList?.places != null && MainActivity.placeList!!.places.isNotEmpty()) {
                        val s: Int = MainActivity.placeList!!.places.size
                        var len: Int = 0
                        if (s > 10) {
                            len = 10
                        } else {
                            len = s
                        }
                        //val len = if (s > 10) 10 else s //Limiting to a maximum of 10 right now
                        for (i in 0 until len) {
                            val place: SinglePlace = MainActivity.placeList!!.places[i]
                            if (place.loc == null) {
                                val location = LatLng(
                                    MainActivity.placeList!!.places[i].geometry!!.location!!.latitude!!,
                                    MainActivity.placeList!!.places[i].geometry!!.location!!.longitude!!
                                )
                                addMapMarker(location, place.name, place.vicinity)
                                Log.d(
                                    "TAG2",
                                    "${location}|${place.loc}|${place.name}|${place.vicinity}"
                                )
                            } else {
                                addMapMarker(place.loc, place.name, place.vicinity)

                            }

                        }
                        getDistance()
                    } else {
                        // Display message for lack of results.
                        Toast.makeText(
                            this@MainActivity,
                            "No results found in a 5km radius.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    //addMapMarker(new LatLng(28.6566,77.18432),"Test loc","testing");
                    mMap!!.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            curLocation,
                            14f
                        )
                    )
                    mapReady = true
                    curMarker?.showInfoWindow()
                    //stopLoadingAnimation()
                }
            }

            override fun onFailure(call: Call<PlaceList?>, t: Throwable) {
            }
        })
    }

    fun getDistance() {
        val list: java.util.ArrayList<String> = java.util.ArrayList<String>()

        if (MainActivity.placeList!!.places.isEmpty()) return
        var destination = ""
        for (i in 0 until MainActivity.placeList!!.places.size - 1) {
            val place = MainActivity.placeList!!.places[i]
            destination += place.loc?.latitude.toString() + "," + place.loc?.longitude + "|"
        }
        val place = MainActivity.placeList!!.places[MainActivity.placeList!!.places.size - 1]
        destination += place.loc?.latitude.toString() + "," + place.loc?.longitude
        val params = HashMap<String, String>()
        params["key"] = "AIzaSyAiAir1uMz3NwJDd9vjIhqeEuTUgw2S7VM"
        params["origins"] = curLocation.latitude.toString() + "," + curLocation.longitude
        params["destinations"] = destination
        serviceAPI?.getRestaurantDistances(params)?.enqueue(object :
            Callback<DistanceResult?> {
            override fun onResponse(
                call: Call<DistanceResult?>,
                response: Response<DistanceResult?>
            ) {
                MainActivity.distanceResult = response.body()
                val distanceDurations: java.util.ArrayList<DistanceDuration> =
                    java.util.ArrayList<DistanceDuration>()
                // Log.d("mina",""+ distanceResult.getRows().get(0).getElements());
                if (MainActivity.distanceResult != null) {
                    //  distanceDurations = distanceResult.getRows().get(0).getElements();
                    if (distanceDurations == null) return
                    for (i in distanceDurations.indices) {
                        val d: DistanceDuration = distanceDurations[i]
//                        list.add(DistanceDuration(d))


//                        Log.d(TAG, "onResponse: distance"+d.getDistance().getText());
//                        Log.d(TAG, "onResponse: duration"+d.getDuration().getText());
                        placeList!!.places[i].distance = d.distance!!.value
                        placeList!!.places[i].distanceString = d.distance!!.text
                        placeList!!.places[i].timeMinutes = d.duration!!.value
                        placeList!!.places[i].timeString = d.duration!!.text

                        list.add(d.distance!!.text.toString())
//                        Log.d("TAG3", "${list[1]}")
//                        list.reverse()
//                        Log.d("TAG3", "${list[1]}")


                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to fetch data from the server. Please try again later",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DistanceResult?>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Unable to access server. Please try again later",
                    Toast.LENGTH_SHORT
                ).show()
                //                Log.d(TAG, "onFailure: cannot fetch distances");
            }
        })
    }

}