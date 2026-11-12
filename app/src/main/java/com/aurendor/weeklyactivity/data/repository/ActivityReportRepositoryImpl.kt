package com.aurendor.weeklyactivity.data.repository

import com.aurendor.weeklyactivity.data.datasource.AppInstallDataSource
import com.aurendor.weeklyactivity.data.datasource.UsageStatsDataSource
import com.aurendor.weeklyactivity.data.local.dao.AppEventDao
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import com.aurendor.weeklyactivity.data.local.entity.AppEventEntity
import com.aurendor.weeklyactivity.di.IoDispatcher
import com.aurendor.weeklyactivity.domain.model.ActivityReport
import com.aurendor.weeklyactivity.domain.model.AppUsage
import com.aurendor.weeklyactivity.domain.model.InstallStats
import com.aurendor.weeklyactivity.domain.repository.ActivityReportRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@Singleton
class ActivityReportRepositoryImpl
@Inject
constructor(
    private val usageStatsDataSource: UsageStatsDataSource,
    private val appInstallDataSource: AppInstallDataSource,
    private val notificationDao: NotificationDao,
    private val appEventDao: AppEventDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ActivityReportRepository {
    override suspend fun getActivityReport(
        startTime: Instant,
        endTime: Instant,
        includeNotifications: Boolean
    ): ActivityReport = withContext(ioDispatcher) {
        val startMillis = startTime.toEpochMilli()
        val endMillis = endTime.toEpochMilli()

        // Clean up data older than retention window before querying
        notificationDao.deleteOlderThan(startMillis)
        appEventDao.deleteOlderThan(startMillis)

        coroutineScope {
            val usageDeferred =
                async {
                    usageStatsDataSource.getUsageMetrics(startTime, endTime)
                }
            val installsDeferred =
                async {
                    appInstallDataSource.getInstalledCount(startTime)
                }
            val removalsDeferred =
                async {
                    appEventDao.countEventsBetween(
                        AppEventEntity.TYPE_REMOVED,
                        startMillis,
                        endMillis
                    )
                }
            val notificationsDeferred =
                if (includeNotifications) {
                    async { notificationDao.countBetween(startMillis, endMillis) }
                } else {
                    null
                }

            val usageMetrics = usageDeferred.await()
            val installedCount = installsDeferred.await()
            val removedCount = removalsDeferred.await()
            val notificationCount = notificationsDeferred?.await()

            val topAppsWithLabels = resolveTopApps(usageMetrics.appDurations)

            val totalUsage =
                usageMetrics.dailyUsage.fold(Duration.ZERO) { acc, daily ->
                    acc + daily.duration
                }

            ActivityReport(
                totalForegroundUsage = totalUsage,
                dailyUsage = usageMetrics.dailyUsage,
                topApps = topAppsWithLabels,
                installStats =
                InstallStats(
                    installedCount = installedCount,
                    removedCount = removedCount
                ),
                notificationCount = notificationCount
            )
        }
    }

    private suspend fun resolveTopApps(appDurations: Map<String, Duration>): List<AppUsage> =
        appDurations.entries
            .filter { it.value > Duration.ZERO }
            .sortedByDescending { it.value }
            .take(MAX_TOP_APPS)
            .map { (pkg, duration) ->
                AppUsage(
                    packageName = pkg,
                    appName = appInstallDataSource.getAppLabel(pkg),
                    usageDuration = duration
                )
            }

    companion object {
        private const val MAX_TOP_APPS = 10
    }
}
