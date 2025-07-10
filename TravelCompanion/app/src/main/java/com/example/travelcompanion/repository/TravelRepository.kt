package com.example.travelcompanion.repository

import com.example.travelcompanion.database.dao.LocationDao
import com.example.travelcompanion.database.dao.UserDao
import com.example.travelcompanion.database.dao.TripDao
import com.example.travelcompanion.database.dao.ImageDao
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.Image

class TravelRepository(
    private val userDao: UserDao,
    private val locationDao: LocationDao,
    private val tripDao: TripDao,
    val imageDao: ImageDao
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

    // Trip methods
    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    suspend fun getAllTrips(): List<Trip> {
        return tripDao.getAllTrips()
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }

    suspend fun completeTrip(tripId: Long, roundedDuration: Double) {
        val trip = tripDao.getTripById(tripId)
        if (trip != null) {
            val updatedTrip = trip.copy(
                duration = roundedDuration,
                inProgress = false
            )
            tripDao.updateTrip(updatedTrip)
        }
    }
    
    // Image operations
    suspend fun insertImage(image: Image): Long {
        return imageDao.insertImage(image)
    }
    
    suspend fun getImageById(id: Long): Image? {
        return imageDao.getImageById(id)
    }
    
    suspend fun updateImage(image: Image) {
        imageDao.updateImage(image)
    }
    
    suspend fun deleteImage(image: Image) {
        imageDao.deleteImage(image)
    }
    
    suspend fun deleteImageById(id: Long) {
        imageDao.deleteImageById(id)
    }


}