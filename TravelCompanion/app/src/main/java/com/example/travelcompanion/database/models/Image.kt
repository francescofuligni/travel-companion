package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entità per le immagini associate ai viaggi
 */
@Entity(
    tableName = "images",
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
data class Image(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long?,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)