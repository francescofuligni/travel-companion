package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entità per gli utenti del sistema
 */
@Entity(
    tableName = "user",
    foreignKeys = [
        ForeignKey(
            entity = Location::class,
            parentColumns = ["id"],
            childColumns = ["homeLocationId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Image::class,
            parentColumns = ["id"],
            childColumns = ["profilePictureId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["homeLocationId"]), Index(value = ["profilePictureId"])]
)
data class User(
    @PrimaryKey
    val id: Long,
    val name: String,
    val email: String,
    val homeLocationId: Long? = null,
    val profilePictureId: Long? = null
)