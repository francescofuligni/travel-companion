package com.example.travelcompanion.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.travelcompanion.database.entities.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: Int): User?

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<User>

    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()
}