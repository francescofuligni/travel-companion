package com.example.travelcompanion.ui.home

import android.content.Intent
import android.content.Context
import androidx.core.content.ContextCompat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.TrackingService
import kotlinx.coroutines.launch
import java.util.Date

class NewTripViewModel(
    private val repository: TravelRepository,
    private val context: Context
) : ViewModel() {

    fun startTrip(title: String, destination: String, type: String, endDate: Date) {
        val startDate = System.currentTimeMillis()
        val endDateMillis = endDate.time

        val trip = Trip(
            id = 0,
            title = title,
            isActive = true,
            type = TripType.fromString(type) ?: TripType.LOCAL, // fallback sicuro
            destination = destination,
            startDate = startDate,
            endDate = endDateMillis,
            duration = 0.0,
            distance = 0.0
        )

        viewModelScope.launch {
            val tripId = repository.insertTrip(trip)

            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                putExtra("TRIP_ID", tripId.toString())
                putExtra("END_DATE", endDateMillis)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
