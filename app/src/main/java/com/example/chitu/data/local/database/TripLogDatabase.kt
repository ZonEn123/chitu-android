package com.example.chitu.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.chitu.data.local.dao.TripLogDao
import com.example.chitu.data.local.entity.TripLog

@Database(
    entities = [TripLog::class],
    version = 1,
    exportSchema = false
)
abstract class TripLogDatabase : RoomDatabase() {

    abstract fun tripLogDao(): TripLogDao

    companion object {
        @Volatile
        private var INSTANCE: TripLogDatabase? = null

        fun getInstance(context: Context): TripLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripLogDatabase::class.java,
                    "trip_log_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}