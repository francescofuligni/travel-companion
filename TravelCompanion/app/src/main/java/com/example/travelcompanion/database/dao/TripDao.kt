package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.travelcompanion.database.entities.Trip

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Query("SELECT * FROM trip")
    suspend fun getAllTrips(): List<Trip>

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trip")
    suspend fun deleteAllTrips()
}