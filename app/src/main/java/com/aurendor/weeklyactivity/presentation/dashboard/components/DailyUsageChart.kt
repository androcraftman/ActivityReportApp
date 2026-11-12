package com.aurendor.weeklyactivity.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aurendor.weeklyactivity.presentation.dashboard.DailyBarUi
import com.aurendor.weeklyactivity.ui.theme.WeeklyActivityTheme

private val PeakDayColor = Color(0xFF1E88E5)
private val RegularDayColor = Color(0xFF90CAF9)
private val ChartHeight = 110.dp
private const val MIN_BAR_RATIO = 0.06f
private const val BAR_WIDTH_FRACTION = 0.75f

@Composable
fun DailyUsageChart(
    dailyBars: List<DailyBarUi>,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Daily app usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Total foreground time per day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyBars.forEach { bar ->
                    DailyBarColumn(
                        bar = bar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyBarColumn(
    bar: DailyBarUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val ratio = if (bar.heightRatio > 0f) {
                bar.heightRatio.coerceAtLeast(MIN_BAR_RATIO)
            } else {
                0f
            }
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(BAR_WIDTH_FRACTION)
                        .fillMaxHeight(ratio)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (bar.isPeakDay) PeakDayColor else RegularDayColor)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = bar.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (bar.isPeakDay) FontWeight.Bold else FontWeight.Normal,
            color = if (bar.isPeakDay) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
fun DailyUsageChartPreview() {
    WeeklyActivityTheme {
        DailyUsageChart(
            dailyBars = listOf(
                DailyBarUi("Mon", "2h 10m", 0.35f, isPeakDay = false),
                DailyBarUi("Tue", "3h 40m", 0.55f, isPeakDay = false),
                DailyBarUi("Wed", "4h 15m", 0.65f, isPeakDay = false),
                DailyBarUi("Thu", "3h 05m", 0.45f, isPeakDay = false),
                DailyBarUi("Fri", "6h 20m", 1.0f, isPeakDay = true),
                DailyBarUi("Sat", "4h 50m", 0.70f, isPeakDay = false),
                DailyBarUi("Sun", "2h 15m", 0.32f, isPeakDay = false)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
