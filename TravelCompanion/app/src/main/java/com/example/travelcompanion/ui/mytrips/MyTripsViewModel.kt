package com.example.travelcompanion.ui.mytrips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ViewModel per gestire i dati dei viaggi con filtri per anno e tipo
 */
class MyTripsViewModel(private val repository: TravelRepository) : ViewModel() {
    private val _trips = MutableLiveData<List<Trip>>()

    private val _tripUiModels = MutableLiveData<List<TripUiModel>>()
    val tripUiModels: LiveData<List<TripUiModel>> = _tripUiModels

    private val _selectedYear = MutableLiveData<Int>()

    private val _selectedType = MutableLiveData<TripType?>(null)

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
            val trips = when {
                type == null -> repository.getTripsByYear(year)
                else -> repository.getTripsByYearAndType(year, type)
            }
            _trips.value = trips
            // Build UI models with first image URI for each trip
            val uiModels = withContext(Dispatchers.Default) {
                trips.map { trip ->
                    val firstImage = repository.getFirstImageForTrip(trip.id)
                    TripUiModel(
                        id = trip.id,
                        title = trip.title,
                        destination = trip.destination,
                        imageUrl = firstImage?.uri,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        type = trip.type,
                        distance = trip.distance,
                        duration = trip.duration
                    )
                }
            }
            _tripUiModels.value = uiModels
        }
    }

    /**
     * Resetta tutti i filtri
     */
    fun resetFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        setYear(currentYear)
        setType(null)
    }
}
