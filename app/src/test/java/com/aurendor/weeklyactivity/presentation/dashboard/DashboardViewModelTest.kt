package com.aurendor.weeklyactivity.presentation.dashboard

import app.cash.turbine.test
import com.aurendor.weeklyactivity.domain.model.ActivityReport
import com.aurendor.weeklyactivity.domain.model.InstallStats
import com.aurendor.weeklyactivity.domain.model.PermissionStatus
import com.aurendor.weeklyactivity.domain.usecase.CheckPermissionsUseCase
import com.aurendor.weeklyactivity.domain.usecase.GetActivityReportUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val getActivityReportUseCase: GetActivityReportUseCase = mockk()
    private val checkPermissionsUseCase: CheckPermissionsUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `usage permission missing sets usage section to PermissionRequired`() =
        runTest(testDispatcher) {
            every { checkPermissionsUseCase() } returns
                PermissionStatus(
                    hasUsageAccess = false,
                    hasNotificationAccess = false
                )

            val viewModel =
                DashboardViewModel(
                    getActivityReportUseCase = getActivityReportUseCase,
                    checkPermissionsUseCase = checkPermissionsUseCase
                )

            viewModel.uiState.test {
                testScheduler.advanceUntilIdle()
                val state = expectMostRecentItem()
                assertTrue(state.usageSection is SectionState.PermissionRequired)
                assertTrue(state.installSection is SectionState.PermissionRequired)
                assertTrue(state.notificationSection is SectionState.PermissionRequired)
            }
        }

    @Test
    fun `open usage settings intent emits NavigateToUsageAccessSettings effect`() =
        runTest(testDispatcher) {
            every { checkPermissionsUseCase() } returns
                PermissionStatus(
                    hasUsageAccess = true,
                    hasNotificationAccess = true
                )
            val dummyReport =
                ActivityReport(
                    totalForegroundUsage = 10.hours,
                    dailyUsage = emptyList(),
                    topApps = emptyList(),
                    installStats = InstallStats(1, 0),
                    notificationCount = 5
                )
            coEvery { getActivityReportUseCase() } returns Result.success(dummyReport)

            val viewModel =
                DashboardViewModel(
                    getActivityReportUseCase = getActivityReportUseCase,
                    checkPermissionsUseCase = checkPermissionsUseCase
                )

            viewModel.effects.test {
                viewModel.processIntent(DashboardIntent.OpenUsageSettings)
                testScheduler.advanceUntilIdle()
                val effect = awaitItem()
                assertEquals(DashboardEffect.NavigateToUsageAccessSettings, effect)
            }
        }
}
