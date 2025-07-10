package com.example.travelcompanion.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.TrackingService
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    fun stopTrip(tripId: Long, startDateMillis: Long, context: Context) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val durationMillis = now - startDateMillis
            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
            val roundedDuration = hours + if (minutes >= 30) 0.5 else 0.0

            repository.completeTrip(tripId, roundedDuration)

            val stopIntent = Intent(context, TrackingService::class.java)
            context.stopService(stopIntent)
        }
    }
}