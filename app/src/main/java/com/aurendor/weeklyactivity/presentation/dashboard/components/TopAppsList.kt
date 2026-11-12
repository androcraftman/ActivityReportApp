package com.aurendor.weeklyactivity.presentation.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aurendor.weeklyactivity.presentation.dashboard.AppUsageUi
import com.aurendor.weeklyactivity.ui.theme.WeeklyActivityTheme

private val ProgressBarColor = Color(0xFF1E88E5)
private const val INITIAL_VISIBLE_COUNT = 5

@Composable
fun TopAppsList(
    apps: List<AppUsageUi>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            TopAppsHeader()
            Spacer(modifier = Modifier.height(16.dp))
            TopAppsContent(
                apps = apps,
                expanded = expanded,
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun TopAppsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Top 10 applications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "By usage time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopAppsContent(
    apps: List<AppUsageUi>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    if (apps.isEmpty()) {
        Text(
            text = "No application usage recorded for this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }

    val initialApps = apps.take(INITIAL_VISIBLE_COUNT)
    val remainingApps = apps.drop(INITIAL_VISIBLE_COUNT)

    initialApps.forEach { app ->
        AppUsageRow(app = app)
        Spacer(modifier = Modifier.height(14.dp))
    }

    if (remainingApps.isNotEmpty()) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                remainingApps.forEach { app ->
                    AppUsageRow(app = app)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
        TopAppsExpandButton(expanded = expanded, onClick = onToggleExpand)
    }
}

@Composable
private fun TopAppsExpandButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector =
                    if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                contentDescription = if (expanded) "Show less" else "Show more",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AppUsageRow(
    app: AppUsageUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "${app.rank}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = app.durationFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(app.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(ProgressBarColor),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
fun TopAppsListPreview() {
    WeeklyActivityTheme {
        TopAppsList(
            apps =
                listOf(
                    AppUsageUi(1, "com.google.android.youtube", "YouTube", "6h 20m", 1.0f),
                    AppUsageUi(2, "com.whatsapp", "WhatsApp", "4h 15m", 0.67f),
                    AppUsageUi(3, "com.instagram.android", "Instagram", "3h 40m", 0.58f),
                    AppUsageUi(4, "com.android.chrome", "Chrome", "2h 30m", 0.39f),
                    AppUsageUi(5, "com.google.android.gm", "Gmail", "1h 45m", 0.28f),
                    AppUsageUi(6, "com.spotify.music", "Spotify", "1h 20m", 0.21f),
                    AppUsageUi(7, "com.google.android.apps.maps", "Maps", "55m", 0.14f),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
