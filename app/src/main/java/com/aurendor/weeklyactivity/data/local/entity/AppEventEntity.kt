package com.aurendor.weeklyactivity.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_events")
data class AppEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val eventType: String,
    val timestamp: Long
) {
    companion object {
        const val TYPE_REMOVED = "REMOVED"
    }
}
