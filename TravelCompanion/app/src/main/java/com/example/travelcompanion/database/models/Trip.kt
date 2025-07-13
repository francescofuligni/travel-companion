package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entità per i viaggi
 */
@Entity(tableName = "trip")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isActive: Boolean,
    val type: TripType,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val duration: Double,
    val distance: Double
)