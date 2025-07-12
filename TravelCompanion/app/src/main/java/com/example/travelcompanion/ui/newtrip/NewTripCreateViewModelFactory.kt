package com.example.travelcompanion.ui.newtrip

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

class NewTripCreateViewModelFactory(
    private val repository: TravelRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewTripCreateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewTripCreateViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}