package com.example.travelcompanion.database.models

import androidx.room.Embedded
import androidx.room.Relation

data class TripPhaseWithLocation(
    @Embedded val phase: TripPhase,
    @Relation(
        parentColumn = "locationId",
        entityColumn = "id"
    )
    val location: Location
)