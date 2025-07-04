package com.example.travelcompanion.database.dao

import androidx.room.*
import com.example.travelcompanion.database.entities.TripPhase
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPhaseDao {
    @Query("SELECT * FROM trip_phases WHERE tripId = :tripId ORDER BY phaseOrder ASC")
    fun getPhasesByTripId(tripId: Long): Flow<List<TripPhase>>

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
}