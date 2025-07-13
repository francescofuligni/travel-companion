package com.example.travelcompanion.repository

import android.content.Context
import android.util.Log
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


    /**
     * Metodi per gestire le location geografiche
     */
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


    /**
     * Metodi per gestire gli utenti
     */
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


    /**
     * Metodi per gestire i viaggi
     */
    suspend fun insertTrip(trip: Trip): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun getActiveTripId(): Long? {
        return tripDao.getActiveTripId()
    }

    suspend fun getTripById(id: Long): Trip? {
        return tripDao.getTripById(id)
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
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

    fun getTripByIdLive(tripId: Long): LiveData<Trip?> {
        return tripDao.getTripByIdLive(tripId)
    }

    suspend fun getTripsByYear(year: Int): List<Trip> {
        return tripDao.getTripsByYear(year.toString())
    }


    /**
     * Metodi per gestire le fasi di un viaggio
     */
    fun getTripPhases(tripId: Long): LiveData<List<TripPhase>> {
        Log.d("TravelRepository", "getTripPhases called for tripId: $tripId")
        return tripPhaseDao.getPhasesByTripId(tripId).asLiveData()
    }

    fun getTripPhasesWithLocations(tripId: Long): LiveData<List<TripPhaseWithLocation>> {
       Log.d(
            "TravelRepository",
            "getTripPhasesWithLocations called for tripId: $tripId"
        )
        return tripPhaseDao.getPhasesWithLocationsByTripId(tripId).asLiveData()
    }

    suspend fun getTripPhasesCount(tripId: Long): Int {
        return try {
            val phases = tripPhaseDao.getPhasesByTripId(tripId)
            var count = 0
            phases.collect { list ->
                count = list.size
                Log.d("TravelRepository", "Found $count phases for trip $tripId")
                return@collect
            }
            count
        } catch (e: Exception) {
            Log.e("TravelRepository", "Error getting phases count", e)
            0
        }
    }


    /**
     * Metodi per gestire le note
     */
    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    fun getNotesByTripId(tripId: Long): LiveData<List<Note>> {
        return noteDao.getNotesByTripIdLiveData(tripId)
    }


    /**
     * Metodi per gestire le immagini
     */
    suspend fun insertTripImage(tripId: Long, imageUri: String) {
        val image = Image(tripId = tripId, uri = imageUri)
        insertImage(image)
    }

    suspend fun getLastTripTimestamp(): Long? {
        return tripDao.getLastTripEndDate()
    }

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