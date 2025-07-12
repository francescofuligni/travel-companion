package com.example.travelcompanion.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

/**
 * Factory per creare HomeActiveTripViewModel con le dipendenze necessarie
 */
class HomeActiveTripViewModelFactory(
    private val application: Application,
    private val repository: TravelRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeActiveTripViewModel::class.java)) {
            return HomeActiveTripViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}