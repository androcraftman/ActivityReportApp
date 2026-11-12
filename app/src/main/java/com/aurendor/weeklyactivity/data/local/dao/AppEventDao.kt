package com.aurendor.weeklyactivity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurendor.weeklyactivity.data.local.entity.AppEventEntity

@Dao
interface AppEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: AppEventEntity)

    @Query("SELECT COUNT(*) FROM app_events WHERE eventType = :eventType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun countEventsBetween(eventType: String, startTime: Long, endTime: Long): Int

    @Query("DELETE FROM app_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
