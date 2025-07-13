package com.example.travelcompanion.ui.mytrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

/**
 * Factory per creare MyTripsViewModel con le dipendenze necessarie
 * Implementa il pattern Factory per l'iniezione del repository
 */
class MyTripsViewModelFactory(private val repository: TravelRepository) : ViewModelProvider.Factory {
    
    /**
     * Crea un'istanza del ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyTripsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyTripsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
