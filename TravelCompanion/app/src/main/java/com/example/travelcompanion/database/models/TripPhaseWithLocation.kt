package com.example.travelcompanion.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe per rappresentare una fase del viaggio con la sua location
 */
data class TripPhaseWithLocation(
    @Embedded val phase: TripPhase,
    @Relation(
        parentColumn = "locationId",
        entityColumn = "id"
    )
    val location: Location
)