package com.example.travelcompanion.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

class SettingsViewModelFactory(
    private val application: Application,
    private val repository: TravelRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}