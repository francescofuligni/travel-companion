package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.travelcompanion.database.models.User

@Dao
interface UserDao {

    /**
     * Restituisce tutti gli utenti
     */
    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<User>

    /**
     * Restituisce un utente per ID
     */
    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    /**
     * Inserisce un nuovo utente
     */
    @Insert
    suspend fun insertUser(user: User)

    /**
     * Aggiorna un utente esistente
     */
    @Update
    suspend fun updateUser(user: User)

    /**
     * Elimina un utente
     */
    @Delete
    suspend fun deleteUser(user: User)

    /**
     * Elimina tutti gli utenti
     */
    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()
}