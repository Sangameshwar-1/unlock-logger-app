package com.example.unlocklogger

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unlocklogger.data.UnlockEvent
import com.example.unlocklogger.data.UnlockEventDao

@Database(entities = [UnlockEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unlockEventDao(): UnlockEventDao

    companion object {
        private const val TAG = "AppDatabase"
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unlock_logger_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d(TAG, "Database onCreate callback triggered")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d(TAG, "Database onOpen callback triggered")
                            
                            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
                            Log.d(TAG, "Tables in database:")
                            while (cursor.moveToNext()) {
                                Log.d(TAG, "  - ${cursor.getString(0)}")
                            }
                            cursor.close()
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                    
                INSTANCE = instance
                Log.d(TAG, "Database instance created")
                instance
            }
        }
    }
}
