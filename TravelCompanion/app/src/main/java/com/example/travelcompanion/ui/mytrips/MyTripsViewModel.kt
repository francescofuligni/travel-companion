package com.example.travelcompanion.ui.mytrips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel per gestire i dati dei viaggi con filtri per anno e tipo
 */
class MyTripsViewModel(private val repository: TravelRepository) : ViewModel() {
    
    private val _trips = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<Trip>> = _trips

    private val _selectedYear = MutableLiveData<Int>()
    val selectedYear: LiveData<Int> = _selectedYear

    private val _selectedType = MutableLiveData<TripType?>(null)
    val selectedType: LiveData<TripType?> = _selectedType

    init {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        _selectedYear.value = currentYear
        loadTrips(year = currentYear, type = null)
    }

    /**
     * Imposta l'anno per il filtro
     */
    fun setYear(year: Int) {
        _selectedYear.value = year
        loadTrips(year, _selectedType.value)
    }

    /**
     * Imposta il tipo di viaggio per il filtro
     */
    fun setType(type: TripType?) {
        _selectedType.value = type
        loadTrips(_selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR), type)
    }

    /**
     * Carica i viaggi applicando i filtri
     */
    private fun loadTrips(year: Int, type: TripType?) {
        viewModelScope.launch {
            _trips.value = when {
                type == null -> repository.getTripsByYear(year)
                else -> repository.getTripsByYearAndType(year, type)
            }
        }
    }

    /**
     * Resetta tutti i filtri
     */
    fun resetFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        _selectedYear.value = currentYear
        _selectedType.value = null
        loadTrips(currentYear, null)
    }
}
