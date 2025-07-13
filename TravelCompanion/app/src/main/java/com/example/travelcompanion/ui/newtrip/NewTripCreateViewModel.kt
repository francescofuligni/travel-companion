package com.example.travelcompanion.ui.newtrip

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.services.TrackingService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NewTripCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TravelRepository.create(getApplication())

    fun startTrip(
        title: String,
        destination: String,
        type: String,
        endDate: java.util.Date,
        destinationLatLng: LatLng?
    ) {
        viewModelScope.launch {
            val appContext = getApplication<Application>()

            // Compute auto-stop timestamp
            val plannedEndDateMillis = endDate.time
            val tripTypeEnum = TripType.fromString(type) ?: TripType.LOCAL
            val actualEndDateMillis: Long = when (tripTypeEnum) {
                TripType.MULTI_DAYS -> plannedEndDateMillis
                TripType.ONE_DAY, TripType.LOCAL -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                else -> -1L
            }

            // Create trip in DB
            val tripId = withContext(Dispatchers.IO) {
                createTripAndGetId(title, destination, type, endDate, destinationLatLng)
            }

            // Start tracking service
            val serviceIntent = Intent(appContext, TrackingService::class.java).apply {
                putExtra("TRIP_ID", tripId.toString())
                if (actualEndDateMillis > 0) putExtra("END_DATE", actualEndDateMillis)
            }
            ContextCompat.startForegroundService(appContext, serviceIntent)
        }
    }

    private suspend fun createTripAndGetId(
        title: String,
        destination: String,
        type: String,
        endDate: java.util.Date,
        destinationLatLng: LatLng?
    ): Long {
        val now = System.currentTimeMillis()
        val tripTypeEnum = TripType.fromString(type) ?: TripType.LOCAL
        val trip = Trip(
            id = 0L,
            title = title,
            isActive = true,
            type = tripTypeEnum,
            destination = destination,
            startDate = now,
            endDate = endDate.time,
            duration = 0.0,
            distance = 0.0
        )
        return repository.insertTrip(trip)
    }
}
