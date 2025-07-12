package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.lifecycle.LiveData
import com.example.travelcompanion.database.models.Image
import kotlinx.coroutines.flow.Flow

/**
 * DAO per le operazioni CRUD sulla tabella images
 */
@Dao
interface ImageDao {
    
    @Insert
    suspend fun insertImage(image: Image): Long

    @Update
    suspend fun updateImage(image: Image)

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): Image?
    
    @Query("SELECT * FROM images WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getImagesByTripId(tripId: Long): Flow<List<Image>>
    
    @Query("SELECT * FROM images WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getImagesByTripIdLiveData(tripId: Long): LiveData<List<Image>>

    @Delete
    suspend fun deleteImage(image: Image)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: Long)
    
    @Query("DELETE FROM images WHERE tripId = :tripId")
    suspend fun deleteImagesByTripId(tripId: Long)

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<Image>
    
    @Query("DELETE FROM images")
    suspend fun deleteAllImages()
}