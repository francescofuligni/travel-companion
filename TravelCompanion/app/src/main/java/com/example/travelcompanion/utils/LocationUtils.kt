package com.example.travelcompanion.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

/**
 * Utility class for handling location operations
 */
object LocationUtils {
    
    /*
     * Default fallback locations
     */
    private val DEFAULT_BOLOGNA_LOCATION = LatLng(44.4949, 11.3426)
    
    /*
     * Gets the current user location if permissions are granted
     * Uses Bologna as fallback location when location cannot be retrieved
     */
    fun getCurrentLocation(
        context: Context,
        onSuccess: (LatLng) -> Unit,
        onFailure: (LatLng) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    onSuccess(currentLatLng)
                } else {
                    onFailure(DEFAULT_BOLOGNA_LOCATION)
                }
            }.addOnFailureListener {
                onFailure(DEFAULT_BOLOGNA_LOCATION)
            }
        } else {
            onFailure(DEFAULT_BOLOGNA_LOCATION)
        }
    }

    
    /*
     * Checks if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /*
     * Gets the default Bologna location for settings screen
     */
    fun getDefaultBolognaLocation(): LatLng {
        return DEFAULT_BOLOGNA_LOCATION
    }
}
