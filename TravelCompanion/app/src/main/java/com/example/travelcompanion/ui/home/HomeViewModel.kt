package com.example.travelcompanion.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.TrackingService
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.travelcompanion.database.models.Trip

class HomeViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    private val _activeTrip = MutableLiveData<Trip?>()
    val activeTrip: LiveData<Trip?> = _activeTrip

    fun checkActiveTrip() {
        viewModelScope.launch {
            val activeId = repository.getActiveTripId()
            val active = activeId?.let { repository.getTripById(it) }
            _activeTrip.postValue(active)
        }
    }
}