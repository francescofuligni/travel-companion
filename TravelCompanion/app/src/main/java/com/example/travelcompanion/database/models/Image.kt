package com.example.travelcompanion.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image")
data class Image(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)