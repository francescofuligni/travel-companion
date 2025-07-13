package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.travelcompanion.database.models.Trip

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip) : Long

    @Query("SELECT * FROM trip")
    suspend fun getAllTrips(): List<Trip>

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Query("SELECT * FROM trip WHERE id = :id")
    fun getTripByIdLive(id: Long): androidx.lifecycle.LiveData<Trip?>

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trip")
    suspend fun deleteAllTrips()

    @Query("SELECT id FROM trip WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTripId(): Long?

    @Query("SELECT * FROM trip WHERE strftime('%Y', datetime(startDate / 1000, 'unixepoch')) = :year")
    suspend fun getTripsByYear(year: String): List<Trip>

    @Query("SELECT * FROM trip WHERE type = :type")
    suspend fun getTripsByType(type: String): List<Trip>

    @Query("SELECT * FROM trip WHERE strftime('%Y', datetime(startDate / 1000, 'unixepoch')) = :year AND type = :type")
    suspend fun getTripsByYearAndType(year: String, type: String): List<Trip>
    @Query("SELECT endDate FROM trip ORDER BY endDate DESC LIMIT 1")
    suspend fun getLastTripEndDate(): Long?
}