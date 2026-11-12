package com.aurendor.weeklyactivity.domain.usecase

import com.aurendor.weeklyactivity.domain.model.ActivityReport
import com.aurendor.weeklyactivity.domain.model.ActivityReportError
import com.aurendor.weeklyactivity.domain.model.AppUsage
import com.aurendor.weeklyactivity.domain.model.DailyUsage
import com.aurendor.weeklyactivity.domain.model.InstallStats
import com.aurendor.weeklyactivity.domain.repository.ActivityReportRepository
import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GetActivityReportUseCaseTest {
    private val repository: ActivityReportRepository = mockk()
    private val permissionRepository: PermissionRepository = mockk()
    private val fixedInstant = Instant.parse("2026-11-12T12:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val useCase =
        GetActivityReportUseCase(
            activityReportRepository = repository,
            permissionRepository = permissionRepository,
            clock = clock
        )

    @Test
    fun `invoke returns failure and skips repository when usage permission is denied`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns false

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ActivityReportError.UsagePermissionRequired)
        coVerify(exactly = 0) { repository.getActivityReport(any(), any(), any()) }
        verify(exactly = 0) { permissionRepository.hasNotificationAccess() }
    }

    @Test
    fun `invoke queries repository with exact 7 local calendar day window bounds`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val expectedStart = Instant.parse("2026-11-06T00:00:00Z")
        val expectedEnd = fixedInstant
        val dummyReport = createDummyReport()
        coEvery { repository.getActivityReport(expectedStart, expectedEnd, true) } returns
            dummyReport

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.getActivityReport(expectedStart, expectedEnd, true) }
    }

    @Test
    fun `invoke queries repository with exact 7 local calendar day window in non-UTC timezone`() =
        runTest {
            val laZone = ZoneId.of("America/Los_Angeles")
            val laInstant = Instant.parse("2026-11-12T02:00:00Z")
            val laClock = Clock.fixed(laInstant, laZone)
            val laUseCase =
                GetActivityReportUseCase(
                    activityReportRepository = repository,
                    permissionRepository = permissionRepository,
                    clock = laClock
                )

            every { permissionRepository.hasUsageAccess() } returns true
            every { permissionRepository.hasNotificationAccess() } returns true
            val expectedStart = Instant.parse("2026-11-05T08:00:00Z")
            val expectedEnd = laInstant
            coEvery { repository.getActivityReport(expectedStart, expectedEnd, any()) } returns
                createDummyReport()

            val result = laUseCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.getActivityReport(expectedStart, expectedEnd, any())
            }
        }

    @Test
    fun `invoke passes includeNotifications false when notification access denied`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns false
        val dummyReport = createDummyReport(notificationCount = null)
        coEvery { repository.getActivityReport(any(), any(), false) } returns dummyReport

        val result = useCase().getOrThrow()

        assertNull(result.notificationCount)
        coVerify(exactly = 1) { repository.getActivityReport(any(), any(), false) }
    }

    @Test
    fun `invoke rethrows CancellationException when coroutine cancelled`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        coEvery { repository.getActivityReport(any(), any(), any()) } throws
            CancellationException("Cancelled")

        try {
            useCase()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }

    @Test
    fun `invoke returns failure when repository throws exception`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val error = IOException("Network / disk error")
        coEvery { repository.getActivityReport(any(), any(), any()) } throws error

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke sorts apps by duration, filters zero, and computes percentage`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val apps =
            listOf(
                AppUsage("app.b", "App B", 2.hours),
                AppUsage("app.zero", "App Zero", Duration.ZERO),
                AppUsage("app.a", "App A", 4.hours),
                AppUsage("app.c", "App C", 1.hours)
            )
        val dummyReport = createDummyReport(topApps = apps)
        coEvery { repository.getActivityReport(any(), any(), true) } returns dummyReport

        val result = useCase().getOrThrow()

        assertEquals(3, result.topApps.size)
        assertEquals("app.a", result.topApps[0].packageName)
        assertEquals(1.0f, result.topApps[0].percentageOfTop, 0.001f)
        assertEquals("app.b", result.topApps[1].packageName)
        assertEquals(0.5f, result.topApps[1].percentageOfTop, 0.001f)
        assertEquals("app.c", result.topApps[2].packageName)
        assertEquals(0.25f, result.topApps[2].percentageOfTop, 0.001f)
    }

    @Test
    fun `invoke limits top apps to maximum 10`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val apps =
            (1..15).map { index ->
                AppUsage("app.$index", "App $index", index.hours)
            }
        val dummyReport = createDummyReport(topApps = apps)
        coEvery { repository.getActivityReport(any(), any(), true) } returns dummyReport

        val result = useCase().getOrThrow()

        assertEquals(10, result.topApps.size)
        assertEquals("app.15", result.topApps[0].packageName)
    }

    @Test
    fun `invoke correctly flags peak day and flags all tied days`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val dailyList =
            listOf(
                DailyUsage(LocalDate.of(2026, 11, 6), 5.hours),
                DailyUsage(LocalDate.of(2026, 11, 7), 2.hours),
                DailyUsage(LocalDate.of(2026, 11, 8), 5.hours)
            )
        val dummyReport = createDummyReport(dailyUsage = dailyList)
        coEvery { repository.getActivityReport(any(), any(), true) } returns dummyReport

        val result = useCase().getOrThrow()

        assertTrue(result.dailyUsage[0].isPeakDay)
        assertFalse(result.dailyUsage[1].isPeakDay)
        assertTrue(result.dailyUsage[2].isPeakDay)
    }

    @Test
    fun `invoke with all-zero daily usage results in no peak day`() = runTest {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true
        val dailyList =
            listOf(
                DailyUsage(LocalDate.of(2026, 11, 6), Duration.ZERO),
                DailyUsage(LocalDate.of(2026, 11, 7), Duration.ZERO)
            )
        val dummyReport = createDummyReport(dailyUsage = dailyList, topApps = emptyList())
        coEvery { repository.getActivityReport(any(), any(), true) } returns dummyReport

        val result = useCase().getOrThrow()

        assertTrue(result.dailyUsage.none { it.isPeakDay })
        assertTrue(result.topApps.isEmpty())
    }

    private fun createDummyReport(
        dailyUsage: List<DailyUsage> = emptyList(),
        topApps: List<AppUsage> = emptyList(),
        notificationCount: Int? = 10
    ) = ActivityReport(
        totalForegroundUsage = 10.hours,
        dailyUsage = dailyUsage,
        topApps = topApps,
        installStats = InstallStats(2, 1),
        notificationCount = notificationCount
    )
}
