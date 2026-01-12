package com.example.unlocklogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UnlockDao {
    @Insert
    suspend fun insertEvent(event: UnlockEvent)

    @Query("SELECT COUNT(*) FROM unlock_events")
    suspend fun getUnlockCount(): Int

    @Query("SELECT * FROM unlock_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<UnlockEvent>
}
