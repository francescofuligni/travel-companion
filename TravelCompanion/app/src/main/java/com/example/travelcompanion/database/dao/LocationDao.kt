package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.travelcompanion.database.entities.Location

@Dao
interface LocationDao {

    @Insert
    suspend fun insertLocation(location: Location)

    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<Location>

    @Delete
    suspend fun deleteLocation(location: Location)
}