package com.example.travelcompanion.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "trip_phases",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Location::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId"), Index("locationId")]
)
data class TripPhase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val locationId: Long,
    val phaseOrder: Int,
    val arrivalDate: String? = null,
    val departureDate: String? = null,
    val notes: String? = null
)