package com.example.travelcompanion.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe per rappresentare un viaggio con le sue fasi
 */
data class TripWithPhases(
    @Embedded val trip: Trip,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val phases: List<TripPhase>
)