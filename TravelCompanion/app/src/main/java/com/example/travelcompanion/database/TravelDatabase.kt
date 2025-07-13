package com.example.travelcompanion.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.travelcompanion.database.dao.*
import com.example.travelcompanion.database.models.*

/**
 * Database principale dell'applicazione Travel Companion
 */
@Database(
    entities = [User::class, Location::class, Trip::class, Image::class, TripPhase::class, Note::class],
    version = 4,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun tripDao(): TripDao
    abstract fun imageDao(): ImageDao
    abstract fun tripPhaseDao(): TripPhaseDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: TravelDatabase? = null

        fun getDatabase(context: Context): TravelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}