package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entità per le note associate ai viaggi
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
