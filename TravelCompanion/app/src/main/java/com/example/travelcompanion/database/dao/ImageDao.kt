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

    /**
     * Restituisce tutte le immagini
     */
    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<Image>

    /**
     * Restituisce un'immagine per ID
     */
    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): Image?

    /**
     * Restituisce le immagini di un viaggio ordinate per data di creazione
     */
    @Query("SELECT * FROM images WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getImagesByTripId(tripId: Long): Flow<List<Image>>

    /**
     * Restituisce le immagini di un viaggio con LiveData
     */
    @Query("SELECT * FROM images WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getImagesByTripIdLiveData(tripId: Long): LiveData<List<Image>>

    /**
     * Restituisce la prima immagine (più vecchia) associata a un viaggio
     */
    @Query("SELECT * FROM images WHERE tripId = :tripId ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstImageForTrip(tripId: Long): Image?

    /**
     * Inserisce una nuova immagine
     */
    @Insert
    suspend fun insertImage(image: Image): Long

    /**
     * Aggiorna un'immagine esistente
     */
    @Update
    suspend fun updateImage(image: Image)

    /**
     * Elimina un'immagine
     */
    @Delete
    suspend fun deleteImage(image: Image)

    /**
     * Elimina un'immagine per ID
     */
    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    /**
     * Elimina tutte le immagini di un viaggio
     */
    @Query("DELETE FROM images WHERE tripId = :tripId")
    suspend fun deleteImagesByTripId(tripId: Long)

    /**
     * Elimina tutte le immagini
     */
    @Query("DELETE FROM images")
    suspend fun deleteAllImages()
}