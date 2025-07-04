package com.example.travelcompanion.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.travelcompanion.database.dao.*
import com.example.travelcompanion.database.entities.*

@Database(
    entities = [Location::class, User::class, Trip::class, TripPhase::class],
    version = 1,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun tripPhaseDao(): TripPhaseDao

    companion object {
        @Volatile
        private var INSTANCE: TravelDatabase? = null

        fun getDatabase(context: Context): TravelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}