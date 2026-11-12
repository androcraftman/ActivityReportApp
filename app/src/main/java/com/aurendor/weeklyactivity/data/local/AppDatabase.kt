package com.aurendor.weeklyactivity.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aurendor.weeklyactivity.data.local.dao.AppEventDao
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import com.aurendor.weeklyactivity.data.local.entity.AppEventEntity
import com.aurendor.weeklyactivity.data.local.entity.NotificationEntity

@Database(
    entities = [
        NotificationEntity::class,
        AppEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appEventDao(): AppEventDao

    companion object {
        const val DATABASE_NAME = "activity_report.db"
    }
}
