package com.aurendor.weeklyactivity.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration

data class DailyUsage(
    val dayOfWeek: DayOfWeek,
    val date: LocalDate,
    val duration: Duration,
    val isPeakDay: Boolean = false
)
