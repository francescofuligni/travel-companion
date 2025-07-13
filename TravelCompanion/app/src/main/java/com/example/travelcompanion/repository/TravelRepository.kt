package com.example.travelcompanion.repository

import android.content.Context
import com.example.travelcompanion.database.dao.LocationDao
import com.example.travelcompanion.database.dao.UserDao
import com.example.travelcompanion.database.dao.TripDao
import com.example.travelcompanion.database.dao.ImageDao
import com.example.travelcompanion.database.dao.TripPhaseDao
import com.example.travelcompanion.database.dao.NoteDao
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.database.models.Note
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.database.models.TripPhase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.asLiveData
import com.example.travelcompanion.database.TravelDatabase
import com.example.travelcompanion.database.models.TripPhaseWithLocation
import kotlinx.coroutines.Dispatchers

class TravelRepository(
    private val userDao: UserDao,
    private val locationDao: LocationDao,
    private val tripDao: TripDao,
    val imageDao: ImageDao,
    private val tripPhaseDao: TripPhaseDao,
    private val noteDao: NoteDao
) {
    // Location methods
    suspend fun insertLocation(location: Location): Long {
        return locationDao.insertLocation(location)
    }

    suspend fun getLocationById(id: Long): Location? {
        return locationDao.getLocationById(id)
    }

    fun getLocationByIdLiveData(id: Long): LiveData<Location?> {
        return liveData(Dispatchers.IO) {
            emit(locationDao.getLocationById(id))
        }
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

    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }

    // Trip methods
    suspend fun insertTrip(trip: Trip): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun getLastTripEndDate(): Long? {
        return tripDao.getLastTripEndDate()
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

    suspend fun getFirstImageForTrip(tripId: Long): Image? {
        return imageDao.getFirstImageForTrip(tripId)
    }

    suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }

    suspend fun getTripByIdLive(tripId: Long): androidx.lifecycle.LiveData<Trip?> {
        return tripDao.getTripByIdLive(tripId)
    }

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
    fun getTripPhases(tripId: Long): LiveData<List<TripPhase>> {
        android.util.Log.d("TravelRepository", "getTripPhases called for tripId: $tripId")
        return tripPhaseDao.getPhasesByTripId(tripId).asLiveData()
    }

    fun getTripPhasesWithLocations(tripId: Long): LiveData<List<TripPhaseWithLocation>> {
        android.util.Log.d(
            "TravelRepository",
            "getTripPhasesWithLocations called for tripId: $tripId"
        )
        return tripPhaseDao.getPhasesWithLocationsByTripId(tripId).asLiveData()
    }

    /**
     * DEBUG method to get phases count
     */
    suspend fun getTripPhasesCount(tripId: Long): Int {
        return try {
            val phases = tripPhaseDao.getPhasesByTripId(tripId)
            var count = 0
            phases.collect { list ->
                count = list.size
                android.util.Log.d("TravelRepository", "Found $count phases for trip $tripId")
                return@collect
            }
            count
        } catch (e: Exception) {
            android.util.Log.e("TravelRepository", "Error getting phases count", e)
            0
        }
    }

    suspend fun insertTripPhase(phase: TripPhase): Long {
        return tripPhaseDao.insertPhase(phase)
    }

    suspend fun getLatestTripPhase(tripId: Long): TripPhase? {
        return tripPhaseDao.getLatestPhase(tripId)
    }

    // Note methods
    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    fun getNotesByTripId(tripId: Long): LiveData<List<Note>> {
        return noteDao.getNotesByTripIdLiveData(tripId)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    // Image methods for trips
    suspend fun insertTripImage(tripId: Long, imageUri: String) {
        val image = Image(tripId = tripId, uri = imageUri)
        insertImage(image)
    }

    suspend fun getLastTripTimestamp(): Long? {
        return tripDao.getLastTripEndDate()
    }

    // Image methods
    suspend fun insertImage(image: Image): Long {
        return imageDao.insertImage(image)
    }

    suspend fun getImageById(id: Long): Image? {
        return imageDao.getImageById(id)
    }

    fun getImagesByTripId(tripId: Long): LiveData<List<Image>> {
        return imageDao.getImagesByTripIdLiveData(tripId)
    }

    suspend fun deleteImage(image: Image) {
        imageDao.deleteImage(image)
    }

    companion object {
        fun create(context: Context): TravelRepository {
            val database = TravelDatabase.getDatabase(context)
            return TravelRepository(
                userDao = database.userDao(),
                locationDao = database.locationDao(),
                tripDao = database.tripDao(),
                imageDao = database.imageDao(),
                tripPhaseDao = database.tripPhaseDao(),
                noteDao = database.noteDao()
            )
        }
    }
}