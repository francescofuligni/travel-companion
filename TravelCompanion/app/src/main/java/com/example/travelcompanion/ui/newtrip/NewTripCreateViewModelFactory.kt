package com.example.travelcompanion.ui.newtrip

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NewTripCreateViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewTripCreateViewModel::class.java)) {
            return NewTripCreateViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
