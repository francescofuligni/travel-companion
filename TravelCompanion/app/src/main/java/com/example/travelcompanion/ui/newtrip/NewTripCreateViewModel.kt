package com.example.travelcompanion.ui.newtrip

import android.content.Intent
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.TrackingService
import com.google.android.gms.maps.model.LatLng
import com.example.travelcompanion.services.TrackingService
import kotlinx.coroutines.launch
import java.util.Date

class NewTripCreateViewModel(
    private val repository: TravelRepository,
    private val context: Context
) : ViewModel() {

    fun startTrip(title: String, destination: String, type: String, endDate: Date, destinationLatLng: LatLng? = null) {
        val startDate = System.currentTimeMillis()
        val plannedEndDateMillis = endDate.time

        val trip = Trip(
            id = 0,
            title = title,
            isActive = true,
            type = TripType.fromString(type) ?: TripType.LOCAL, // fallback sicuro
            destination = destination,
            startDate = startDate,
            endDate = 0L, // Trip is active, no end date yet
            duration = 0.0,
            distance = 0.0
        )

        viewModelScope.launch {
            val tripId = repository.insertTrip(trip)
            android.util.Log.d("NewTripCreateViewModel", "Trip created with ID: $tripId")

            // Se è stato fornito un LatLng per la destinazione, salvalo come Location
            destinationLatLng?.let { coords ->
                val destinationLocation = Location(
                    latitude = coords.latitude,
                    longitude = coords.longitude
                )
                repository.insertLocation(destinationLocation)
            }

            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                putExtra("TRIP_ID", tripId.toString())
                putExtra("END_DATE", plannedEndDateMillis)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            android.util.Log.d("NewTripCreateViewModel", "Tracking service started for trip: $tripId")
        }
    }
}
