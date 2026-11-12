package com.aurendor.weeklyactivity.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurendor.weeklyactivity.domain.model.ActivityReport
import com.aurendor.weeklyactivity.domain.model.ActivityReportError
import com.aurendor.weeklyactivity.domain.usecase.CheckPermissionsUseCase
import com.aurendor.weeklyactivity.domain.usecase.GetActivityReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getActivityReportUseCase: GetActivityReportUseCase,
    private val checkPermissionsUseCase: CheckPermissionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        processIntent(DashboardIntent.LoadReport)
    }

    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadReport -> loadReport(isRefresh = false)
            is DashboardIntent.Refresh -> loadReport(isRefresh = true)
            is DashboardIntent.OpenUsageSettings -> {
                emitEffect(DashboardEffect.NavigateToUsageAccessSettings)
            }
            is DashboardIntent.OpenNotificationSettings -> {
                emitEffect(DashboardEffect.NavigateToNotificationAccessSettings)
            }
        }
    }

    private fun loadReport(isRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { current ->
                if (isRefresh) {
                    current.copy(isRefreshing = true)
                } else {
                    current.copy(
                        usageSection = SectionState.Loading,
                        installSection = SectionState.Loading,
                        notificationSection = SectionState.Loading
                    )
                }
            }

            val permissions = checkPermissionsUseCase()
            if (!permissions.hasUsageAccess) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        usageSection = SectionState.PermissionRequired,
                        installSection = SectionState.PermissionRequired,
                        notificationSection = SectionState.PermissionRequired
                    )
                }
                return@launch
            }

            val result = getActivityReportUseCase()
            result.fold(
                onSuccess = { report ->
                    updateWithReport(report, permissions.hasNotificationAccess)
                },
                onFailure = { error ->
                    val errorState = if (error is ActivityReportError.UsagePermissionRequired) {
                        SectionState.PermissionRequired
                    } else {
                        SectionState.Error(error.message ?: "Failed to load activity report")
                    }
                    _uiState.update { current ->
                        current.copy(
                            isRefreshing = false,
                            usageSection = errorState,
                            installSection = errorState,
                            notificationSection = errorState
                        )
                    }
                }
            )
        }
    }

    private fun updateWithReport(report: ActivityReport, hasNotificationAccess: Boolean) {
        val peakDuration = report.dailyUsage.maxOfOrNull { it.duration } ?: Duration.ZERO

        val dailyBars = report.dailyUsage.map { daily ->
            val ratio = if (peakDuration > Duration.ZERO) {
                (daily.duration / peakDuration).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            DailyBarUi(
                dayLabel = daily.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                durationFormatted = formatDuration(daily.duration),
                heightRatio = ratio,
                isPeakDay = daily.isPeakDay
            )
        }

        val topApps = report.topApps.mapIndexed { index, app ->
            AppUsageUi(
                rank = index + 1,
                packageName = app.packageName,
                appName = app.appName,
                durationFormatted = formatDuration(app.usageDuration),
                progress = app.percentageOfTop
            )
        }

        val usageUi = UsageReportUi(
            totalUsageFormatted = formatDuration(report.totalForegroundUsage),
            dailyBars = dailyBars,
            topApps = topApps
        )

        val installUi = InstallStatsUi(
            total = report.installStats.totalEvents,
            installedCount = report.installStats.installedCount,
            removedCount = report.installStats.removedCount
        )

        val notificationSectionState = when {
            report.notificationCount != null -> SectionState.Success(NotificationUi(report.notificationCount))
            !hasNotificationAccess -> SectionState.PermissionRequired
            else -> SectionState.Success(NotificationUi(0))
        }

        _uiState.update {
            it.copy(
                isRefreshing = false,
                usageSection = SectionState.Success(usageUi),
                installSection = SectionState.Success(installUi),
                notificationSection = notificationSectionState
            )
        }
    }

    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.inWholeHours
        val minutes = duration.inWholeMinutes % MINUTES_PER_HOUR
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    companion object {
        private const val MINUTES_PER_HOUR = 60L
    }
}
