package com.aurendor.weeklyactivity.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A notification captured by the listener service.
 * [notificationKey] = StatusBarNotification.key. The unique index means a notification and its
 * updates (progress, edits, reposts) are counted once.
 * Trade-off: apps that reuse one key for new content (e.g. a chat thread) count once per
 * report window. Accepted as a simplification. Ongoing and group-summary notifications are
 * filtered out in the listener service, not here.
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["notificationKey"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationKey: String,
    val packageName: String,
    val timestamp: Long
)
