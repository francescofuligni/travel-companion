package com.example.travelcompanion.ui.home

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripPhase
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.services.TrackingService
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
     */
    suspend fun getTripById(tripId: Long): LiveData<Trip?> {
        return repository.getTripByIdLive(tripId)
    }
    
    /**
     * Ferma il viaggio attivo
     */
    fun stopTrip(tripId: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                stopTrackingService()
                val currentTrip = repository.getTripById(tripId)
                if (currentTrip == null) {
                    Log.e("HomeActiveTripViewModel", "Trip non trovato per ID=$tripId")
                    onComplete(false)
                    return@launch
                }
                val updatedTrip = currentTrip.copy(
                    endDate = System.currentTimeMillis(),
                    isActive = false
                )
                repository.updateTrip(updatedTrip)
                _trip.value = updatedTrip
                Log.d("HomeActiveTripViewModel", "Viaggio terminato con successo: ID=$tripId")
                onComplete(true)
            } catch (e: Exception) {
                Log.e("HomeActiveTripViewModel", "Errore terminazione viaggio", e)
                onComplete(false)
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
     */
    fun getTripPhases(tripId: Long): LiveData<List<TripPhase>> {
        return repository.getTripPhases(tripId)
    }
    
    /**
     * Ottiene una location per ID
     */
    fun getLocationById(locationId: Long): LiveData<Location?> {
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