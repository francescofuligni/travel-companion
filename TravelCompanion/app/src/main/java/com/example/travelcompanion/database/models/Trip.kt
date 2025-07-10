package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String? = null,
    val startDate: String,
    val endDate: String
)