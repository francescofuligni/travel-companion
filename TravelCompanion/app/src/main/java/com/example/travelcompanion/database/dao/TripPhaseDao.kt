package com.example.travelcompanion.database.dao

import androidx.room.*
import com.example.travelcompanion.database.models.TripPhase
import com.example.travelcompanion.database.models.TripPhaseWithLocation
import kotlinx.coroutines.flow.Flow

/**
 * DAO per le operazioni CRUD sulla tabella trip_phases
 */
@Dao
interface TripPhaseDao {
    
    /**
     * Restituisce tutte le fasi di un viaggio ordinate per ordine di fase
     */
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY phaseOrder ASC")
    fun getPhasesByTripId(tripId: Long): Flow<List<TripPhase>>

    /**
     * Restituisce le fasi di un viaggio con le relative location
     */
    @Transaction
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY phaseOrder ASC")
    fun getPhasesWithLocationsByTripId(tripId: Long): Flow<List<TripPhaseWithLocation>>

    /**
     * Restituisce una fase per ID
     */
    @Query("SELECT * FROM trip_phases WHERE id = :id")
    suspend fun getPhaseById(id: Long): TripPhase?

    /**
     * Restituisce l'ultima fase prima di un determinato ordine
     */
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId AND phaseOrder < :order ORDER BY phaseOrder DESC LIMIT 1")
    suspend fun getLastPhaseBefore(tripId: Long, order: Int): TripPhase?

    /**
     * Restituisce l'ultima fase inserita per un viaggio
     */
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPhase(tripId: Long): TripPhase?

    /**
     * Inserisce una nuova fase
     */
    @Insert
    suspend fun insertPhase(phase: TripPhase): Long

    /**
     * Aggiorna una fase esistente
     */
    @Update
    suspend fun updatePhase(phase: TripPhase)

    /**
     * Elimina una fase
     */
    @Delete
    suspend fun deletePhase(phase: TripPhase)

    /**
     * Elimina tutte le fasi di un viaggio
     */
    @Query("DELETE FROM trip_phases WHERE tripId = :tripId")
    suspend fun deletePhasesByTripId(tripId: Long)

    /**
     * Elimina tutte le fasi
     */
    @Query("DELETE FROM trip_phases")
    suspend fun deleteAllPhases()
}