package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.travelcompanion.database.models.Location

@Dao
interface LocationDao {

    @Insert
    suspend fun insertLocation(location: Location) : Long

    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<Location>

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun getLocationById(id: Long): Location?

    @Query("SELECT * FROM location WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax LIMIT 1")
    suspend fun findWithinRadius(latMin: Double, latMax: Double, lonMin: Double, lonMax: Double): Location?

    @Delete
    suspend fun deleteLocation(location: Location)

    @Query("DELETE FROM location")
    suspend fun deleteAllLocations()
}