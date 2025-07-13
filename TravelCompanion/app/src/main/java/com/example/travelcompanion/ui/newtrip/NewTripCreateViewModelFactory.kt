package com.example.travelcompanion.ui.newtrip

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory per creare NewTripCreateViewModel con le dipendenze necessarie
 * Implementa il pattern Factory per l'iniezione dell'Application context
 */
class NewTripCreateViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea un'istanza del ViewModel con l'Application context
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewTripCreateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewTripCreateViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
