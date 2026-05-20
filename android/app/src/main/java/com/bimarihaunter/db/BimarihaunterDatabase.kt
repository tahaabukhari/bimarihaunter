package com.bimarihaunter.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [OutbreakReportEntity::class], version = 2, exportSchema = false)
@TypeConverters(OutbreakConverters::class)
abstract class BimarihaunterDatabase : RoomDatabase() {
    abstract fun outbreakReportDao(): OutbreakReportDao
    
    companion object {
        @Volatile
        private var INSTANCE: BimarihaunterDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE outbreak_reports ADD COLUMN locations TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }
        
        fun getDatabase(context: Context): BimarihaunterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BimarihaunterDatabase::class.java,
                    "bimarihaunter_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
