package com.aurendor.weeklyactivity.presentation.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurendor.weeklyactivity.presentation.dashboard.components.DailyUsageChart
import com.aurendor.weeklyactivity.presentation.dashboard.components.DashboardHeader
import com.aurendor.weeklyactivity.presentation.dashboard.components.PermissionCard
import com.aurendor.weeklyactivity.presentation.dashboard.components.StatCardsRow
import com.aurendor.weeklyactivity.presentation.dashboard.components.TopAppsList
import com.aurendor.weeklyactivity.presentation.dashboard.components.TotalUsageCard
import com.aurendor.weeklyactivity.ui.theme.WeeklyActivityTheme

@Composable
fun DashboardRoute(modifier: Modifier = Modifier, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.processIntent(DashboardIntent.Refresh)
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToUsageAccessSettings -> {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                is DashboardEffect.NavigateToNotificationAccessSettings -> {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        onIntent = viewModel::processIntent,
        modifier = modifier
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onIntent: (DashboardIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        DashboardHeader()

        when (val usageState = uiState.usageSection) {
            is SectionState.PermissionRequired -> {
                Spacer(modifier = Modifier.height(24.dp))
                PermissionCard(
                    onOpenUsageSettings = { onIntent(DashboardIntent.OpenUsageSettings) }
                )
            }
            is SectionState.Loading -> {
                LoadingContent()
            }
            is SectionState.Error -> {
                Spacer(modifier = Modifier.height(24.dp))
                ErrorCard(
                    message = usageState.message,
                    onRetry = { onIntent(DashboardIntent.Refresh) }
                )
            }
            is SectionState.Success -> {
                DashboardSuccessContent(
                    uiState = uiState,
                    usage = usageState.data,
                    onIntent = onIntent
                )
            }
        }
    }
}

@Composable
private fun DashboardSuccessContent(
    uiState: DashboardUiState,
    usage: UsageReportUi,
    onIntent: (DashboardIntent) -> Unit
) {
    TotalUsageCard(totalUsageFormatted = usage.totalUsageFormatted)
    Spacer(modifier = Modifier.height(8.dp))
    StatCardsRow(
        installSection = uiState.installSection,
        notificationSection = uiState.notificationSection,
        onOpenNotificationSettings = { onIntent(DashboardIntent.OpenNotificationSettings) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    DailyUsageChart(dailyBars = usage.dailyBars)
    Spacer(modifier = Modifier.height(16.dp))
    TopAppsList(apps = usage.topApps)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Failed to load report",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
fun DashboardScreenSuccessPreview() {
    WeeklyActivityTheme {
        DashboardScreen(
            uiState =
            DashboardUiState(
                usageSection =
                SectionState.Success(
                    UsageReportUi(
                        totalUsageFormatted = "24h 35m",
                        dailyBars =
                        listOf(
                            DailyBarUi("Mon", "2h", 0.3f, false),
                            DailyBarUi("Tue", "3h", 0.5f, false),
                            DailyBarUi("Wed", "4h", 0.7f, false),
                            DailyBarUi("Thu", "2h", 0.3f, false),
                            DailyBarUi("Fri", "6h", 1.0f, true),
                            DailyBarUi("Sat", "4h", 0.7f, false),
                            DailyBarUi("Sun", "3h", 0.5f, false)
                        ),
                        topApps =
                        listOf(
                            AppUsageUi(1, "com.youtube", "YouTube", "6h 20m", 1.0f),
                            AppUsageUi(2, "com.whatsapp", "WhatsApp", "4h 15m", 0.67f)
                        )
                    )
                ),
                installSection = SectionState.Success(InstallStatsUi(4, 3, 1)),
                notificationSection = SectionState.Success(NotificationUi(286))
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPermissionRequiredPreview() {
    WeeklyActivityTheme {
        DashboardScreen(
            uiState =
            DashboardUiState(
                usageSection = SectionState.PermissionRequired,
                installSection = SectionState.PermissionRequired,
                notificationSection = SectionState.PermissionRequired
            ),
            onIntent = {}
        )
    }
}
