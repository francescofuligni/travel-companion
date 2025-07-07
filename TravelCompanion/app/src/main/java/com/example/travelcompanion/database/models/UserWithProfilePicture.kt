package com.example.travelcompanion.database.models

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithProfilePicture(
    @Embedded val user: User,
    @Relation(
        parentColumn = "profilePictureId",
        entityColumn = "id"
    )
    val profilePicture: Image?
)