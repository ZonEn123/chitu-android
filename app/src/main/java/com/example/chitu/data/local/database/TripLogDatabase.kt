package com.example.chitu.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.chitu.data.local.dao.TripLogDao
import com.example.chitu.data.local.entity.TripLog

@Database(
    entities = [TripLog::class],
    version = 4,
    exportSchema = false
)
abstract class TripLogDatabase : RoomDatabase() {

    abstract fun tripLogDao(): TripLogDao

    companion object {

        // Migration：从版本1升级到版本2（新增 clientId、serverId、syncStatus）
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trip_log ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE trip_log ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE trip_log ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration：从版本2升级到版本3（新增经纬度字段）
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trip_log ADD COLUMN startLatitude REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE trip_log ADD COLUMN startLongitude REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE trip_log ADD COLUMN endLatitude REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE trip_log ADD COLUMN endLongitude REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Migration：从版本3升级到版本4（新增 userId 字段）
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trip_log ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: TripLogDatabase? = null

        fun getInstance(context: Context): TripLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripLogDatabase::class.java,
                    "trip_log_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
