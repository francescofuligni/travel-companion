package com.example.travelcompanion.repository

import android.content.Context

import com.example.travelcompanion.database.dao.LocationDao
import com.example.travelcompanion.database.dao.UserDao
import com.example.travelcompanion.database.dao.TripDao
import com.example.travelcompanion.database.dao.ImageDao
import com.example.travelcompanion.database.dao.TripPhaseDao
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.database.models.TripPhase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers

class TravelRepository(
    private val userDao: UserDao,
    private val locationDao: LocationDao,
    private val tripDao: TripDao,
    val imageDao: ImageDao,
    private val tripPhaseDao: TripPhaseDao
) {
    // Location methods
    suspend fun insertLocation(location: Location): Long {
        return locationDao.insertLocation(location)
    }

    suspend fun getAllLocations(): List<Location> {
        return locationDao.getAllLocations()
    }

    suspend fun getLocationById(id: Long): Location? {
        return locationDao.getLocationById(id)
    }

    /**
     * Ottiene una location per ID come LiveData
     */
    fun getLocationByIdLiveData(id: Long): LiveData<Location?> {
        return liveData(Dispatchers.IO) {
            emit(locationDao.getLocationById(id))
        }
    }

    suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location)
    }

    suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }

    // User methods
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun getUserById(id: Long): User? {
        return userDao.getUserById(id)
    }

    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }

    // Image methods
    suspend fun insertImage(image: Image): Long {
        return imageDao.insertImage(image)
    }

    suspend fun getImageById(id: Long): Image? {
        return imageDao.getImageById(id)
    }

    suspend fun deleteImage(image: Image) {
        imageDao.deleteImage(image)
    }


    // Trip methods
    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    suspend fun getActiveTripId(): Long? {
        return tripDao.getActiveTripId()
    }

    suspend fun getTripById(id: Long): Trip? {
        return tripDao.getTripById(id)
    }

    suspend fun getAllTrips(): List<Trip> {
        return tripDao.getAllTrips()
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }

    suspend fun getTripsByType(type: TripType): List<Trip> {
        return tripDao.getTripsByType(type.name)
    }

    suspend fun getTripsByYearAndType(year: Int, type: TripType): List<Trip> {
        return tripDao.getTripsByYearAndType(year.toString(), type.name)
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }

    /**
     * Termina un viaggio attivo
     * @param tripId ID del viaggio da terminare
     * @param duration Durata finale del viaggio in secondi
     */
    suspend fun endTrip(tripId: Long, duration: Double) {
        val trip = tripDao.getTripById(tripId)
        if (trip != null) {
            val updatedTrip = trip.copy(
                isActive = false,
                duration = duration,
                endDate = System.currentTimeMillis()
            )
            tripDao.updateTrip(updatedTrip)
        }
    }

    suspend fun getTripsByYear(year: Int): List<Trip> {
        return tripDao.getTripsByYear(year.toString())
    }

    // TripPhase methods
    /**
     * Ottiene le fasi di un viaggio come LiveData
     */
    fun getTripPhases(tripId: Long): LiveData<List<TripPhase>> {
        return tripPhaseDao.getPhasesByTripId(tripId).asLiveData()
    }

    /**
     * Inserisce una nuova fase del viaggio
     */
    suspend fun insertTripPhase(phase: TripPhase): Long {
        return tripPhaseDao.insertPhase(phase)
    }
    
    companion object {
        fun create(context: Context): TravelRepository {
            val database = com.example.travelcompanion.database.TravelDatabase.getDatabase(context)
            return TravelRepository(
                userDao = database.userDao(),
                locationDao = database.locationDao(),
                tripDao = database.tripDao(),
                imageDao = database.imageDao(),
                tripPhaseDao = database.tripPhaseDao()
            )
        }
    }
}