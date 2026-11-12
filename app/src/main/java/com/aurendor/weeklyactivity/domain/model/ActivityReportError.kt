package com.aurendor.weeklyactivity.domain.model

sealed class ActivityReportError(message: String) : Exception(message) {
    data object UsagePermissionRequired : ActivityReportError("Usage access permission is required")
}
