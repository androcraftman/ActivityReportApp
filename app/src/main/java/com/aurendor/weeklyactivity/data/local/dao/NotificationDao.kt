package com.aurendor.weeklyactivity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurendor.weeklyactivity.data.local.entity.NotificationEntity

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT COUNT(*) FROM notifications WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun countBetween(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM notifications WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
