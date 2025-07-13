package com.example.travelcompanion.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

/**
 * Factory per creare SettingsViewModel con le dipendenze necessarie
 */
class SettingsViewModelFactory(
    private val application: Application,
    private val repository: TravelRepository
) : ViewModelProvider.Factory {
    
    /**
     * Crea un'istanza del ViewModel con le dipendenze iniettate
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}