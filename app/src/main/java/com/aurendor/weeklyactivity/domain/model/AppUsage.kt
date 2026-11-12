package com.aurendor.weeklyactivity.domain.model

import kotlin.time.Duration

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageDuration: Duration,
    val percentageOfTop: Float = 0f
)
