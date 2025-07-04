package com.example.travelcompanion.repository

import com.example.travelcompanion.database.dao.LocationDao
import com.example.travelcompanion.database.dao.UserDao
import com.example.travelcompanion.database.dao.TripDao
import com.example.travelcompanion.database.entities.Location
import com.example.travelcompanion.database.entities.User
import com.example.travelcompanion.database.entities.Trip

class TravelRepository(
    private val locationDao: LocationDao,
    private val userDao: UserDao,
    private val tripDao: TripDao
) {
    // Location methods
    suspend fun insertLocation(location: Location) {
        locationDao.insertLocation(location)
    }

    suspend fun getAllLocations(): List<Location> {
        return locationDao.getAllLocations()
    }

    suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location)
    }

    // User methods
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
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
}