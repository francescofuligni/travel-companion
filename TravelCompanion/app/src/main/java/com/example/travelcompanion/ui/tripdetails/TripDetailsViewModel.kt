package com.example.travelcompanion.ui.tripdetails

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripPhase
import com.example.travelcompanion.database.models.Note
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.launch

class TripDetailsViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    private val _tripId = MutableLiveData<Long>()
    
    private val _trip = MutableLiveData<Trip?>()
    val trip: LiveData<Trip?> = _trip

    // Use switchMap to automatically handle LiveData changes
    val tripPhases: LiveData<List<TripPhase>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading phases for trip ID: $tripId")
        repository.getTripPhases(tripId)
    }

    val tripPhasesWithLocations: LiveData<List<com.example.travelcompanion.database.models.TripPhaseWithLocation>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading phases with locations for trip ID: $tripId")
        repository.getTripPhasesWithLocations(tripId)
    }

    val tripNotes: LiveData<List<Note>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading notes for trip ID: $tripId")
        repository.getNotesByTripId(tripId)
    }

    val tripImages: LiveData<List<Image>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading images for trip ID: $tripId")
        repository.getImagesByTripId(tripId)
    }

    fun loadTripDetails(tripId: Long) {
        Log.d("TripDetailsViewModel", "loadTripDetails called with tripId: $tripId")
        _tripId.value = tripId
        viewModelScope.launch {
            // Load trip details
            val tripData = repository.getTripById(tripId)
            Log.d("TripDetailsViewModel", "Trip loaded: ${tripData?.title}")
            _trip.value = tripData
            
            // Debug: manually check phases count
            val phasesCount = repository.getTripPhasesCount(tripId)
            Log.d("TripDetailsViewModel", "Manual phases count: $phasesCount")
        }
    }
}
