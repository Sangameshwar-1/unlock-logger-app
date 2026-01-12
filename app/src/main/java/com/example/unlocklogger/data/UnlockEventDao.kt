package com.example.unlocklogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UnlockEventDao {
    @Insert
    suspend fun insert(event: UnlockEvent)
    
    @Query("SELECT COUNT(*) FROM unlock_events")
    suspend fun getUnlockCount(): Int
    
    @Query("SELECT * FROM unlock_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<UnlockEvent>
    
    @Query("SELECT * FROM unlock_events WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getEventsSince(startTime: Long): List<UnlockEvent>
    
    @Query("DELETE FROM unlock_events")
    suspend fun deleteAll()
}
