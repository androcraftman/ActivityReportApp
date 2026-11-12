package com.aurendor.weeklyactivity.data.repository

import com.aurendor.weeklyactivity.data.datasource.AppInstallDataSource
import com.aurendor.weeklyactivity.data.datasource.UsageMetricsResult
import com.aurendor.weeklyactivity.data.datasource.UsageStatsDataSource
import com.aurendor.weeklyactivity.data.local.dao.AppEventDao
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import com.aurendor.weeklyactivity.data.local.entity.AppEventEntity
import com.aurendor.weeklyactivity.domain.model.DailyUsage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

class ActivityReportRepositoryImplTest {
    private val usageStatsDataSource: UsageStatsDataSource = mockk()
    private val appInstallDataSource: AppInstallDataSource = mockk()
    private val notificationDao: NotificationDao = mockk()
    private val appEventDao: AppEventDao = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val repository =
        ActivityReportRepositoryImpl(
            usageStatsDataSource = usageStatsDataSource,
            appInstallDataSource = appInstallDataSource,
            notificationDao = notificationDao,
            appEventDao = appEventDao,
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `getActivityReport with notifications off returns null count and skips DAO`() =
        runTest(testDispatcher) {
            val startTime = Instant.parse("2026-11-06T00:00:00Z")
            val endTime = Instant.parse("2026-11-12T12:00:00Z")
            val startMillis = startTime.toEpochMilli()

            coEvery { notificationDao.deleteOlderThan(startMillis) } returns 0
            coEvery { appEventDao.deleteOlderThan(startMillis) } returns 0

            val dailyList =
                listOf(
                    DailyUsage(LocalDate.of(2026, 11, 6), 2.hours),
                    DailyUsage(LocalDate.of(2026, 11, 7), 3.hours),
                )
            val appDurations =
                mapOf(
                    "com.example.youtube" to 3.hours,
                    "com.example.chat" to 2.hours,
                )
            coEvery { usageStatsDataSource.getUsageMetrics(startTime, endTime) } returns
                UsageMetricsResult(
                    dailyUsage = dailyList,
                    appDurations = appDurations,
                )
            coEvery { appInstallDataSource.getInstalledCount(startTime) } returns 3
            coEvery { appEventDao.countEventsBetween(AppEventEntity.TYPE_REMOVED, any(), any()) } returns 1
            coEvery { appInstallDataSource.getAppLabel("com.example.youtube") } returns "YouTube"
            coEvery { appInstallDataSource.getAppLabel("com.example.chat") } returns "Chat"

            val report =
                repository.getActivityReport(
                    startTime = startTime,
                    endTime = endTime,
                    includeNotifications = false,
                )

            assertNull(report.notificationCount)
            coVerify(exactly = 0) { notificationDao.countBetween(any(), any()) }
            coVerify(exactly = 1) { notificationDao.deleteOlderThan(startMillis) }
            coVerify(exactly = 1) { appEventDao.deleteOlderThan(startMillis) }

            assertEquals(5.hours, report.totalForegroundUsage)
            assertEquals(3, report.installStats.installedCount)
            assertEquals(1, report.installStats.removedCount)
            assertEquals(2, report.topApps.size)
            assertEquals("YouTube", report.topApps[0].appName)
        }
}
