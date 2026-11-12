package com.aurendor.weeklyactivity.domain.model

import kotlin.time.Duration

data class ActivityReport(
    val totalForegroundUsage: Duration,
    val dailyUsage: List<DailyUsage>,
    val topApps: List<AppUsage>,
    val installStats: InstallStats,
    val notificationCount: Int?
)
