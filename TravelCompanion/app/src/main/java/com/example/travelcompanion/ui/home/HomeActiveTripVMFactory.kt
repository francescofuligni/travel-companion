package com.example.travelcompanion.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

import android.app.Application

class HomeActiveTripVMFactory(
    private val application: Application,
    private val repository: TravelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeActiveTripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeActiveTripViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}