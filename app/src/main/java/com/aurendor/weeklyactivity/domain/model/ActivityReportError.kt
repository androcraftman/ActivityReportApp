package com.aurendor.weeklyactivity.domain.model

sealed class ActivityReportError(message: String) : Exception(message) {
    class UsagePermissionRequired : ActivityReportError("Usage access permission is required")
}
