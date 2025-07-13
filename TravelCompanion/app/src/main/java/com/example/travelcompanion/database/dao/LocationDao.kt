package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.travelcompanion.database.models.Location

@Dao
interface LocationDao {

    /**
     * Restituisce tutte le location
     */
    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<Location>

    /**
     * Restituisce una location per ID
     */
    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun getLocationById(id: Long): Location?

    /**
     * Trova una location entro un raggio specificato
     */
    @Query("SELECT * FROM location WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax LIMIT 1")
    suspend fun findWithinRadius(latMin: Double, latMax: Double, lonMin: Double, lonMax: Double): Location?

    /**
     * Inserisce una nuova location
     */
    @Insert
    suspend fun insertLocation(location: Location): Long

    /**
     * Elimina una location
     */
    @Delete
    suspend fun deleteLocation(location: Location)

    /**
     * Elimina tutte le location
     */
    @Query("DELETE FROM location")
    suspend fun deleteAllLocations()
}