package com.aurendor.weeklyactivity.domain.repository

interface PermissionRepository {
    fun hasUsageAccess(): Boolean

    fun hasNotificationAccess(): Boolean
}
