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

    /**
     * Restituisce tutti i viaggi
     */
    @Query("SELECT * FROM trip")
    suspend fun getAllTrips(): List<Trip>

    /**
     * Restituisce un viaggio per ID
     */
    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    /**
     * Restituisce un viaggio per ID con LiveData
     */
    @Query("SELECT * FROM trip WHERE id = :id")
    fun getTripByIdLive(id: Long): androidx.lifecycle.LiveData<Trip?>

    /**
     * Restituisce l'ID del viaggio attivo
     */
    @Query("SELECT id FROM trip WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTripId(): Long?

    /**
     * Restituisce i viaggi filtrati per anno
     */
    @Query("SELECT * FROM trip WHERE strftime('%Y', datetime(startDate / 1000, 'unixepoch')) = :year")
    suspend fun getTripsByYear(year: String): List<Trip>

    /**
     * Restituisce i viaggi filtrati per tipo
     */
    @Query("SELECT * FROM trip WHERE type = :type")
    suspend fun getTripsByType(type: String): List<Trip>

    /**
     * Restituisce i viaggi filtrati per anno e tipo
     */
    @Query("SELECT * FROM trip WHERE strftime('%Y', datetime(startDate / 1000, 'unixepoch')) = :year AND type = :type")
    suspend fun getTripsByYearAndType(year: String, type: String): List<Trip>

    /**
     * Restituisce la data di fine dell'ultimo viaggio
     */
    @Query("SELECT endDate FROM trip ORDER BY endDate DESC LIMIT 1")
    suspend fun getLastTripEndDate(): Long?

    /**
     * Inserisce un nuovo viaggio
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    /**
     * Aggiorna un viaggio esistente
     */
    @Update
    suspend fun updateTrip(trip: Trip)

    /**
     * Elimina un viaggio
     */
    @Delete
    suspend fun deleteTrip(trip: Trip)

    /**
     * Elimina tutti i viaggi
     */
    @Query("DELETE FROM trip")
    suspend fun deleteAllTrips()
}