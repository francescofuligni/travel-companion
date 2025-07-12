package com.example.travelcompanion.services

import android.app.Service
import android.location.Location
import com.google.android.gms.location.*

abstract class BaseLocationService : Service() {
    protected lateinit var fusedLocationClient: FusedLocationProviderClient

    protected fun initFusedLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    protected fun buildHighAccuracyRequest(intervalMillis: Long, minUpdateMillis: Long): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(minUpdateMillis)
            .build()
    }

    protected fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0].toDouble()
    }
}
