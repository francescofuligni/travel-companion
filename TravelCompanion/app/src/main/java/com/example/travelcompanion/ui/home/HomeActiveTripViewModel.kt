package com.example.travelcompanion.ui.home

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.TrackingService
import kotlinx.coroutines.launch

/**
 * ViewModel per gestire i dati del viaggio attivo
 * Fornisce aggiornamenti real-time dei dati di tracking
 */
class HomeActiveTripViewModel(
    application: Application,
    private val repository: TravelRepository
) : AndroidViewModel(application) {
    
    private val _trip = MutableLiveData<Trip?>()
    val trip: LiveData<Trip?> = _trip
    
    /**
     * Ottiene i dati del viaggio per ID
     * @param tripId ID del viaggio da monitorare
     */
    fun getTripById(tripId: Long): LiveData<Trip?> {
        loadTripData(tripId)
        return trip
    }
    
    /**
     * Carica i dati del viaggio dal repository
     * @param tripId ID del viaggio
     */
    private fun loadTripData(tripId: Long) {
        viewModelScope.launch {
            try {
                val tripData = repository.getTripById(tripId)
                _trip.value = tripData
            } catch (e: Exception) {
                Log.e("HomeActiveTripViewModel", "Errore caricamento viaggio", e)
            }
        }
    }
    
    /**
     * Ferma il viaggio attivo
     * @param tripId ID del viaggio da terminare
     */
    fun stopTrip(tripId: Long) {
        viewModelScope.launch {
            try {
                // Ferma il servizio di tracking prima di aggiornare il database
                stopTrackingService()
                
                // Aggiorna il viaggio con la data/ora di fine
                val currentTrip = repository.getTripById(tripId)
                currentTrip?.let { trip ->
                    val updatedTrip = trip.copy(
                        endDate = System.currentTimeMillis(),
                        isActive = false
                    )
                    repository.updateTrip(updatedTrip)
                    
                    // Aggiorna il LiveData
                    _trip.value = updatedTrip
                    
                    Log.d("HomeActiveTripViewModel", "Viaggio terminato con successo: ID=$tripId")
                }
            } catch (e: Exception) {
                Log.e("HomeActiveTripViewModel", "Errore terminazione viaggio", e)
            }
        }
    }
    
    /**
     * Ferma il servizio di tracking GPS
     */
    private fun stopTrackingService() {
        val intent = Intent(getApplication(), TrackingService::class.java)
        getApplication<Application>().stopService(intent)
    }
    
    /**
     * Ottiene le fasi del viaggio per ID
     * @param tripId ID del viaggio
     */
    fun getTripPhases(tripId: Long): LiveData<List<com.example.travelcompanion.database.models.TripPhase>> {
        return repository.getTripPhases(tripId)
    }
    
    /**
     * Ottiene una location per ID
     * @param locationId ID della location
     */
    fun getLocationById(locationId: Long): LiveData<com.example.travelcompanion.database.models.Location?> {
        return repository.getLocationByIdLiveData(locationId)
    }

    /**
     * Crea un URI per salvare un'immagine
     */
    fun createImageUri(context: android.content.Context): android.net.Uri? {
        return try {
            val fileName = "trip_image_${System.currentTimeMillis()}.jpg"
            val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val imageDetails = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TravelCompanion")
                }
            }
            
            context.contentResolver.insert(imageCollection, imageDetails)
        } catch (e: Exception) {
            Log.e("HomeActiveTripViewModel", "Errore creazione URI immagine", e)
            null
        }
    }

    /**
     * Salva un'immagine al viaggio corrente
     */
    fun saveImageToTrip(tripId: Long, imageUri: String) {
        viewModelScope.launch {
            try {
                repository.insertTripImage(tripId, imageUri)
                Log.d("HomeActiveTripViewModel", "Immagine salvata al viaggio $tripId")
            } catch (e: Exception) {
                Log.e("HomeActiveTripViewModel", "Errore salvataggio immagine", e)
            }
        }
    }

    /**
     * Salva una nota al viaggio corrente
     */
    fun saveNoteToTrip(tripId: Long, noteContent: String) {
        viewModelScope.launch {
            try {
                val note = com.example.travelcompanion.database.models.Note(
                    tripId = tripId,
                    content = noteContent
                )
                repository.insertNote(note)
                Log.d("HomeActiveTripViewModel", "Nota salvata al viaggio $tripId")
            } catch (e: Exception) {
                Log.e("HomeActiveTripViewModel", "Errore salvataggio nota", e)
            }
        }
    }
}