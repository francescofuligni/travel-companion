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
import com.example.travelcompanion.database.models.TripPhaseWithLocation
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.launch

/**
 * ViewModel per gestire i dati dei dettagli di un viaggio
 * Fornisce LiveData reattivi per tutte le informazioni del viaggio
 */
class TripDetailsViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    private val _tripId = MutableLiveData<Long>()
    
    private val _trip = MutableLiveData<Trip?>()
    val trip: LiveData<Trip?> = _trip

    /**
     * Usa switchMap per gestire automaticamente i cambi di LiveData
     * Quando _tripId cambia, le query vengono rieseguite automaticamente
     */
    val tripPhases: LiveData<List<TripPhase>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading phases for trip ID: $tripId")
        repository.getTripPhases(tripId)
    }

    /**
     * LiveData per le fasi del viaggio con le relative posizioni
     */
    val tripPhasesWithLocations: LiveData<List<TripPhaseWithLocation>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading phases with locations for trip ID: $tripId")
        repository.getTripPhasesWithLocations(tripId)
    }

    /**
     * LiveData per le note associate al viaggio
     */
    val tripNotes: LiveData<List<Note>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading notes for trip ID: $tripId")
        repository.getNotesByTripId(tripId)
    }

    /**
     * LiveData per le immagini associate al viaggio
     */
    val tripImages: LiveData<List<Image>> = _tripId.switchMap { tripId ->
        Log.d("TripDetailsViewModel", "Loading images for trip ID: $tripId")
        repository.getImagesByTripId(tripId)
    }

    /**
     * Carica tutti i dettagli del viaggio specificato
     */
    fun loadTripDetails(tripId: Long) {
        Log.d("TripDetailsViewModel", "loadTripDetails called with tripId: $tripId")
        _tripId.value = tripId
        viewModelScope.launch {
            // Carica i dettagli del viaggio
            val tripData = repository.getTripById(tripId)
            Log.d("TripDetailsViewModel", "Trip loaded: ${tripData?.title}")
            _trip.value = tripData
            
            // Debug: verifica manualmente il numero di fasi
            val phasesCount = repository.getTripPhasesCount(tripId)
            Log.d("TripDetailsViewModel", "Manual phases count: $phasesCount")
        }
    }
}
