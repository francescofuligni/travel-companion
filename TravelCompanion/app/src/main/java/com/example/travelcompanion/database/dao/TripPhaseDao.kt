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
    
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY phaseOrder ASC")
    fun getPhasesByTripId(tripId: Long): Flow<List<TripPhase>>

    @Transaction
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY phaseOrder ASC")
    fun getPhasesWithLocationsByTripId(tripId: Long): Flow<List<TripPhaseWithLocation>>

    @Query("SELECT * FROM trip_phases WHERE id = :id")
    suspend fun getPhaseById(id: Long): TripPhase?

    @Insert
    suspend fun insertPhase(phase: TripPhase): Long

    @Update
    suspend fun updatePhase(phase: TripPhase)

    @Delete
    suspend fun deletePhase(phase: TripPhase)

    @Query("DELETE FROM trip_phases WHERE tripId = :tripId")
    suspend fun deletePhasesByTripId(tripId: Long)

    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId AND phaseOrder < :order ORDER BY phaseOrder DESC LIMIT 1")
    suspend fun getLastPhaseBefore(tripId: Long, order: Int): TripPhase?

    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPhase(tripId: Long): TripPhase?
    
    @Query("DELETE FROM trip_phases")
    suspend fun deleteAllPhases()
}