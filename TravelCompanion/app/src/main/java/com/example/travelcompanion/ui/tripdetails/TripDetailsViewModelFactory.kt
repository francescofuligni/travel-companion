package com.example.travelcompanion.ui.tripdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

/**
 * Factory per creare TripDetailsViewModel con le dipendenze necessarie
 * Implementa il pattern Factory per l'iniezione del repository
 */
class TripDetailsViewModelFactory(
    private val repository: TravelRepository
) : ViewModelProvider.Factory {

    /**
     * Crea un'istanza del ViewModel con il repository iniettato
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailsViewModel::class.java)) {
            return TripDetailsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
