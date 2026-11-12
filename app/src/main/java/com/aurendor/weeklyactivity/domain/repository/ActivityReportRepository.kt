package com.aurendor.weeklyactivity.domain.repository

import com.aurendor.weeklyactivity.domain.model.ActivityReport
import java.time.Instant

interface ActivityReportRepository {
    suspend fun getActivityReport(
        startTime: Instant,
        endTime: Instant,
        includeNotifications: Boolean,
    ): ActivityReport
}
