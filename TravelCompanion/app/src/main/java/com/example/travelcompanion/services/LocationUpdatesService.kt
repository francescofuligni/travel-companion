package com.example.travelcompanion.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.travelcompanion.R
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.travelcompanion.services.BaseLocationService
import com.example.travelcompanion.utils.NotificationUtils

class LocationUpdatesService : BaseLocationService() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var placesClient: PlacesClient

    override fun onCreate() {
        super.onCreate()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        initFusedLocationClient()

        locationRequest = buildHighAccuracyRequest(
            intervalMillis = 5 * 60 * 1000L,
            minUpdateMillis = 60 * 1000L
        )

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    checkNearbyPlaces(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // I permessi di localizzazione non sono stati concessi.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun checkNearbyPlaces(location: Location) {
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)

        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // I permessi di localizzazione non sono stati concessi.
            return
        }
        placesClient.findCurrentPlace(request)
            .addOnSuccessListener { response ->
                for (placeLikelihood in response.placeLikelihoods) {
                    val place = placeLikelihood.place
                    val poiLocation = place.latLng ?: continue
                    val distance = calculateDistance(
                        location.latitude, location.longitude,
                        poiLocation.latitude, poiLocation.longitude
                    )
                    if (distance < 10) {
                        NotificationUtils.sendPoiNotification(applicationContext, place.name ?: "Un luogo interessante")
                        break
                    }
                }
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}