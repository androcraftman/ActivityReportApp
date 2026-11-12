package com.aurendor.weeklyactivity.presentation.dashboard

sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data object PermissionRequired : SectionState<Nothing>

    data class Success<T>(
        val data: T,
    ) : SectionState<T>

    data class Error(
        val message: String,
    ) : SectionState<Nothing>
}

data class DailyBarUi(
    val dayLabel: String,
    val durationFormatted: String,
    val heightRatio: Float,
    val isPeakDay: Boolean,
)

data class AppUsageUi(
    val rank: Int,
    val packageName: String,
    val appName: String,
    val durationFormatted: String,
    val progress: Float,
)

data class UsageReportUi(
    val totalUsageFormatted: String,
    val dailyBars: List<DailyBarUi>,
    val topApps: List<AppUsageUi>,
)

data class InstallStatsUi(
    val total: Int,
    val installedCount: Int,
    val removedCount: Int,
)

data class NotificationUi(
    val count: Int,
)

data class DashboardUiState(
    val usageSection: SectionState<UsageReportUi> = SectionState.Loading,
    val installSection: SectionState<InstallStatsUi> = SectionState.Loading,
    val notificationSection: SectionState<NotificationUi> = SectionState.Loading,
    val isRefreshing: Boolean = false,
)

sealed interface DashboardIntent {
    data object LoadReport : DashboardIntent

    data object Refresh : DashboardIntent

    data object OpenUsageSettings : DashboardIntent

    data object OpenNotificationSettings : DashboardIntent
}

sealed interface DashboardEffect {
    data object NavigateToUsageAccessSettings : DashboardEffect

    data object NavigateToNotificationAccessSettings : DashboardEffect
}
