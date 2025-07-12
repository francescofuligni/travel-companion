package com.example.travelcompanion.ui.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.services.TrackingService
import kotlinx.coroutines.launch

class HomeActiveTripViewModel(
    application: Application,
    private val repository: TravelRepository
) : AndroidViewModel(application) {
    fun getTripById(tripId: Long): LiveData<Trip> = liveData {
        val trip = repository.getTripById(tripId)
        trip?.let { emit(it) }
    }

    fun endTrip(tripId: Long, onTripEnded: () -> Unit) {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
            if (trip != null) {
                val endTime = System.currentTimeMillis()

                val duration = (endTime - trip.startDate).toDouble() / 1000
                repository.endTrip(tripId, duration)

                val context = getApplication<Application>()
                val stopIntent = Intent(context, TrackingService::class.java)
                context.stopService(stopIntent)

                onTripEnded()
            }
        }
    }
}