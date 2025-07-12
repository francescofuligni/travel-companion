package com.example.travelcompanion.database.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.travelcompanion.database.models.Note
import kotlinx.coroutines.flow.Flow

/**
 * DAO per le operazioni CRUD sulla tabella notes
 */
@Dao
interface NoteDao {
    
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripId(tripId: Long): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripIdLiveData(tripId: Long): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    @Insert
    suspend fun insertNote(note: Note): Long
    
    @Update
    suspend fun updateNote(note: Note)
    
    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("DELETE FROM notes WHERE tripId = :tripId")
    suspend fun deleteNotesByTripId(tripId: Long)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
