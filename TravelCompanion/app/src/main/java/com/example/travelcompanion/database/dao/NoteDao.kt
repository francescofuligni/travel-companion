package com.example.travelcompanion.database.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.travelcompanion.database.models.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    
    /**
     * Restituisce tutte le note di un viaggio ordinate per timestamp
     */
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripId(tripId: Long): Flow<List<Note>>
    
    /**
     * Restituisce tutte le note di un viaggio con LiveData
     */
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripIdLiveData(tripId: Long): LiveData<List<Note>>
    
    /**
     * Restituisce una nota per ID
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Inserisce una nuova nota
     */
    @Insert
    suspend fun insertNote(note: Note): Long
    
    /**
     * Aggiorna una nota esistente
     */
    @Update
    suspend fun updateNote(note: Note)
    
    /**
     * Elimina una nota
     */
    @Delete
    suspend fun deleteNote(note: Note)
    
    /**
     * Elimina tutte le note di un viaggio
     */
    @Query("DELETE FROM notes WHERE tripId = :tripId")
    suspend fun deleteNotesByTripId(tripId: Long)
    
    /**
     * Elimina tutte le note
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
