package com.aurendor.weeklyactivity.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aurendor.weeklyactivity.presentation.dashboard.InstallStatsUi
import com.aurendor.weeklyactivity.presentation.dashboard.NotificationUi
import com.aurendor.weeklyactivity.presentation.dashboard.SectionState
import com.aurendor.weeklyactivity.ui.theme.WeeklyActivityTheme

private val InstallGreen = Color(0xFF2E7D32)
private val NotificationOrange = Color(0xFFEF6C00)

@Composable
fun StatCardsRow(
    installSection: SectionState<InstallStatsUi>,
    notificationSection: SectionState<NotificationUi>,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstallCard(
            installSection = installSection,
            modifier = Modifier.weight(1f),
        )
        NotificationCard(
            notificationSection = notificationSection,
            onOpenNotificationSettings = onOpenNotificationSettings,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InstallCard(
    installSection: SectionState<InstallStatsUi>,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            InstallIcon(
                tint = InstallGreen,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            InstallCardContent(installSection = installSection)
        }
    }
}

@Composable
private fun InstallCardContent(installSection: SectionState<InstallStatsUi>) {
    when (installSection) {
        is SectionState.Success -> InstallSuccessContent(installSection.data)
        is SectionState.Loading ->
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).padding(vertical = 4.dp),
                strokeWidth = 2.dp,
            )
        else -> {
            Text(
                text = "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Installations",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun InstallSuccessContent(data: InstallStatsUi) {
    Text(
        text = "${data.total}",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = "Installations",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    val detail =
        if (data.removedCount > 0) {
            "${data.installedCount} installed · ${data.removedCount} removed"
        } else {
            "${data.installedCount} installed"
        }
    Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationCard(
    notificationSection: SectionState<NotificationUi>,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = NotificationOrange,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            NotificationCardContent(
                notificationSection = notificationSection,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
        }
    }
}

@Composable
private fun NotificationCardContent(
    notificationSection: SectionState<NotificationUi>,
    onOpenNotificationSettings: () -> Unit,
) {
    when (notificationSection) {
        is SectionState.Success -> NotificationSuccessContent(notificationSection.data)
        is SectionState.PermissionRequired -> NotificationPermissionContent(onOpenNotificationSettings)
        is SectionState.Loading ->
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).padding(vertical = 4.dp),
                strokeWidth = 2.dp,
            )
        is SectionState.Error ->
            Text(
                text = "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
    }
}

@Composable
private fun NotificationSuccessContent(data: NotificationUi) {
    Text(
        text = "${data.count}",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = "Notifications",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = "Received this week",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationPermissionContent(onOpenNotificationSettings: () -> Unit) {
    Text(
        text = "Notifications",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Permission needed",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onOpenNotificationSettings,
        shape = RoundedCornerShape(8.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(
            text = "Grant access",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun InstallIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.3f, h * 0.45f),
            end = Offset(w * 0.5f, h * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.7f, h * 0.45f),
            end = Offset(w * 0.5f, h * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.85f),
            end = Offset(w * 0.8f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
fun StatCardsRowPreview() {
    WeeklyActivityTheme {
        StatCardsRow(
            installSection = SectionState.Success(InstallStatsUi(total = 4, installedCount = 3, removedCount = 1)),
            notificationSection = SectionState.Success(NotificationUi(count = 286)),
            onOpenNotificationSettings = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
fun StatCardsRowPermissionRequiredPreview() {
    WeeklyActivityTheme {
        StatCardsRow(
            installSection = SectionState.Success(InstallStatsUi(total = 4, installedCount = 3, removedCount = 1)),
            notificationSection = SectionState.PermissionRequired,
            onOpenNotificationSettings = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
