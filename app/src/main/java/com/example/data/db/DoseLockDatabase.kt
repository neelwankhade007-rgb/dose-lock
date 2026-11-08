package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DoseLog
import com.example.data.model.Medication

@Database(
    entities = [Medication::class, DoseLog::class],
    version = 3,
    exportSchema = false
)
abstract class DoseLockDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: DoseLockDatabase? = null

        fun getDatabase(context: Context): DoseLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoseLockDatabase::class.java,
                    "doselock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
