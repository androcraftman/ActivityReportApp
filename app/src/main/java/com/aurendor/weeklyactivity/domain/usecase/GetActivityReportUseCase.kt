package com.aurendor.weeklyactivity.domain.usecase

import com.aurendor.weeklyactivity.domain.model.ActivityReport
import com.aurendor.weeklyactivity.domain.model.ActivityReportError
import com.aurendor.weeklyactivity.domain.model.AppUsage
import com.aurendor.weeklyactivity.domain.model.DailyUsage
import com.aurendor.weeklyactivity.domain.repository.ActivityReportRepository
import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration

class GetActivityReportUseCase @Inject constructor(
    private val activityReportRepository: ActivityReportRepository,
    private val permissionRepository: PermissionRepository,
    private val clock: Clock
) {
    @Suppress("TooGenericExceptionCaught") // Result wrapper: catch everything except cancellation (rethrown)
    suspend operator fun invoke(): Result<ActivityReport> {
        return try {
            if (!permissionRepository.hasUsageAccess()) {
                return Result.failure(ActivityReportError.UsagePermissionRequired())
            }

            // Report window covers the last 7 local calendar days up to the current instant
            val today = LocalDate.now(clock)
            val startTime = today.minusDays(REPORT_DAYS - 1).atStartOfDay(clock.zone).toInstant()
            val endTime = clock.instant()
            val includeNotifications = permissionRepository.hasNotificationAccess()

            val rawReport = activityReportRepository.getActivityReport(
                startTime = startTime,
                endTime = endTime,
                includeNotifications = includeNotifications
            )

            val processedDailyUsage = highlightPeakDay(rawReport.dailyUsage)
            val processedTopApps = normalizeAndRankTopApps(rawReport.topApps)

            Result.success(
                rawReport.copy(
                    dailyUsage = processedDailyUsage,
                    topApps = processedTopApps
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun highlightPeakDay(dailyList: List<DailyUsage>): List<DailyUsage> {
        val peakDuration = dailyList.maxOfOrNull { it.duration } ?: Duration.ZERO
        return dailyList.map { daily ->
            daily.copy(isPeakDay = peakDuration > Duration.ZERO && daily.duration == peakDuration)
        }
    }

    private fun normalizeAndRankTopApps(apps: List<AppUsage>): List<AppUsage> {
        val sortedApps = apps
            .filter { it.usageDuration > Duration.ZERO }
            .sortedByDescending { it.usageDuration }
            .take(MAX_TOP_APPS)

        val highestDuration = sortedApps.firstOrNull()?.usageDuration ?: Duration.ZERO

        return sortedApps.map { app ->
            val percentage = if (highestDuration > Duration.ZERO) {
                (app.usageDuration / highestDuration).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            app.copy(percentageOfTop = percentage)
        }
    }

    companion object {
        private const val REPORT_DAYS = 7L
        private const val MAX_TOP_APPS = 10
    }
}
