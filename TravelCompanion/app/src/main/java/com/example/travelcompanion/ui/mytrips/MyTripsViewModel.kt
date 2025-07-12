package com.example.travelcompanion.ui.mytrips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class MyTripsViewModel(private val repository: TravelRepository) : ViewModel() {
    private val _trips = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<Trip>> = _trips

    init {
        loadTrips()
    }

    private fun loadTrips() {
        viewModelScope.launch {
            _trips.value = repository.getAllTrips()
        }
    }

    fun filterTripsByYear(year: Int) {
        viewModelScope.launch {
            val all = repository.getAllTrips()
            val filtered = all.filter { trip ->
                trip.startDate.let { millis ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = millis
                    calendar.get(Calendar.YEAR) == year
                }
            }
            _trips.value = filtered
        }
    }

    fun resetFilter() {
        loadTrips()
    }
}
