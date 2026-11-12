package com.aurendor.weeklyactivity.data.datasource

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class UsageStatsDataSourceTest {
    @Test
    fun `cross-midnight session splits evenly across two days and zero-fills remaining days`() {
        val zone = ZoneOffset.UTC
        val windowStart = Instant.parse("2026-11-06T00:00:00Z")
        val windowEnd = Instant.parse("2026-11-12T12:00:00Z")

        val events =
            listOf(
                RawUsageEvent(
                    packageName = "com.example.app",
                    className = "MainActivity",
                    isForeground = true,
                    timestampMillis = Instant.parse("2026-11-07T23:30:00Z").toEpochMilli(),
                ),
                RawUsageEvent(
                    packageName = "com.example.app",
                    className = "MainActivity",
                    isForeground = false,
                    timestampMillis = Instant.parse("2026-11-08T00:30:00Z").toEpochMilli(),
                ),
            )

        val result =
            UsageStatsDataSource.calculateUsageMetrics(
                events = events,
                windowStart = windowStart,
                windowEnd = windowEnd,
                zone = zone,
                ownPackageName = "com.aurendor.weeklyactivity",
            )

        assertEquals(7, result.dailyUsage.size)

        val dayUsageMap = result.dailyUsage.associate { it.date to it.duration }
        assertEquals(Duration.ZERO, dayUsageMap[LocalDate.of(2026, 11, 6)])
        assertEquals(30.minutes, dayUsageMap[LocalDate.of(2026, 11, 7)])
        assertEquals(30.minutes, dayUsageMap[LocalDate.of(2026, 11, 8)])
        assertEquals(Duration.ZERO, dayUsageMap[LocalDate.of(2026, 11, 9)])
        assertEquals(Duration.ZERO, dayUsageMap[LocalDate.of(2026, 11, 10)])
        assertEquals(Duration.ZERO, dayUsageMap[LocalDate.of(2026, 11, 11)])
        assertEquals(Duration.ZERO, dayUsageMap[LocalDate.of(2026, 11, 12)])

        assertEquals(60.minutes, result.appDurations["com.example.app"])
        assertEquals(60.minutes, result.dailyUsage.map { it.duration }.reduce { a, b -> a + b })
    }
}
