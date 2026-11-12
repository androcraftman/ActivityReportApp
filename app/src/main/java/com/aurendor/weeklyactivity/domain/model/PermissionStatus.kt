package com.aurendor.weeklyactivity.domain.model

data class PermissionStatus(
    val hasUsageAccess: Boolean,
    val hasNotificationAccess: Boolean,
) {
    val isFullyGranted: Boolean get() = hasUsageAccess && hasNotificationAccess
}
