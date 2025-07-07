package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.travelcompanion.database.models.Image

@Dao
interface ImageDao {
    @Insert
    suspend fun insertImage(image: Image): Long

    @Update
    suspend fun updateImage(image: Image)

    @Query("SELECT * FROM image WHERE id = :id")
    suspend fun getImageById(id: Long): Image?

    @Delete
    suspend fun deleteImage(image: Image)

    @Query("DELETE FROM image WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("SELECT * FROM image")
    suspend fun getAllImages(): List<Image>
}