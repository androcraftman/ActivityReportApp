package com.aurendor.weeklyactivity.data.datasource

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.aurendor.weeklyactivity.di.IoDispatcher
import com.aurendor.weeklyactivity.domain.model.DailyUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class RawUsageEvent(
    val packageName: String,
    val className: String,
    val isForeground: Boolean,
    val timestampMillis: Long,
)

data class UsageMetricsResult(
    val dailyUsage: List<DailyUsage>,
    val appDurations: Map<String, Duration>,
)

@Singleton
class UsageStatsDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val clock: Clock,
    ) {
        private val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        suspend fun getUsageMetrics(
            startTime: Instant,
            endTime: Instant,
        ): UsageMetricsResult =
            withContext(ioDispatcher) {
                val rawEvents = queryRawUsageEvents(startTime.toEpochMilli(), endTime.toEpochMilli())
                calculateUsageMetrics(
                    events = rawEvents,
                    windowStart = startTime,
                    windowEnd = endTime,
                    zone = clock.zone,
                    ownPackageName = context.packageName,
                )
            }

        private fun queryRawUsageEvents(
            startMillis: Long,
            endMillis: Long,
        ): List<RawUsageEvent> {
            val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
            val eventList = mutableListOf<RawUsageEvent>()
            val currentEvent = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(currentEvent)
                @Suppress("DEPRECATION")
                val isForeground =
                    when (currentEvent.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        UsageEvents.Event.MOVE_TO_FOREGROUND,
                        -> true
                        UsageEvents.Event.ACTIVITY_PAUSED,
                        UsageEvents.Event.MOVE_TO_BACKGROUND,
                        -> false
                        else -> null
                    }

                if (isForeground != null) {
                    eventList.add(
                        RawUsageEvent(
                            packageName = currentEvent.packageName,
                            className = currentEvent.className ?: "",
                            isForeground = isForeground,
                            timestampMillis = currentEvent.timeStamp,
                        ),
                    )
                }
            }
            return eventList
        }

        companion object {
            fun calculateUsageMetrics(
                events: List<RawUsageEvent>,
                windowStart: Instant,
                windowEnd: Instant,
                zone: ZoneId,
                ownPackageName: String,
            ): UsageMetricsResult {
                val sessionsByPackage =
                    pairSessions(
                        events = events,
                        startMillis = windowStart.toEpochMilli(),
                        endMillis = windowEnd.toEpochMilli(),
                        ownPackageName = ownPackageName,
                    )

                val calendarDays =
                    buildCalendarDays(
                        startDate = LocalDate.ofInstant(windowStart, zone),
                        endDate = LocalDate.ofInstant(windowEnd, zone),
                    )

                val (dailyTotals, appTotals) =
                    distributeSessions(
                        sessionsByPackage = sessionsByPackage,
                        calendarDays = calendarDays,
                        zone = zone,
                    )

                val dailyUsageList =
                    calendarDays.map { day ->
                        DailyUsage(
                            date = day,
                            duration = (dailyTotals[day] ?: 0L).milliseconds,
                        )
                    }

                return UsageMetricsResult(
                    dailyUsage = dailyUsageList,
                    appDurations = appTotals.mapValues { (_, ms) -> ms.milliseconds },
                )
            }

            private fun pairSessions(
                events: List<RawUsageEvent>,
                startMillis: Long,
                endMillis: Long,
                ownPackageName: String,
            ): Map<String, List<Pair<Long, Long>>> {
                val openSessions = mutableMapOf<String, Long>()
                val rawSessions = mutableMapOf<String, MutableList<Pair<Long, Long>>>()

                for (event in events) {
                    if (event.packageName == ownPackageName) continue
                    recordEventSession(event, openSessions, rawSessions, startMillis, endMillis)
                }

                for ((activityKey, sessionStart) in openSessions) {
                    val pkgName = activityKey.substringBefore('/')
                    if (pkgName != ownPackageName && endMillis > sessionStart) {
                        rawSessions
                            .getOrPut(pkgName) { mutableListOf() }
                            .add(sessionStart to endMillis)
                    }
                }

                return rawSessions.mapValues { (_, sessions) -> mergeIntervals(sessions) }
            }

            private fun recordEventSession(
                event: RawUsageEvent,
                openSessions: MutableMap<String, Long>,
                rawSessions: MutableMap<String, MutableList<Pair<Long, Long>>>,
                startMillis: Long,
                endMillis: Long,
            ) {
                val activityKey = "${event.packageName}/${event.className}"
                if (event.isForeground) {
                    openSessions[activityKey] = max(event.timestampMillis, startMillis)
                    return
                }
                val sessionStart = openSessions.remove(activityKey) ?: return
                val sessionEnd = min(event.timestampMillis, endMillis)
                if (sessionEnd > sessionStart) {
                    rawSessions
                        .getOrPut(event.packageName) { mutableListOf() }
                        .add(sessionStart to sessionEnd)
                }
            }

            private fun buildCalendarDays(
                startDate: LocalDate,
                endDate: LocalDate,
            ): List<LocalDate> {
                val days = mutableListOf<LocalDate>()
                var current = startDate
                while (!current.isAfter(endDate)) {
                    days.add(current)
                    current = current.plusDays(1)
                }
                return days
            }

            private fun distributeSessions(
                sessionsByPackage: Map<String, List<Pair<Long, Long>>>,
                calendarDays: List<LocalDate>,
                zone: ZoneId,
            ): Pair<Map<LocalDate, Long>, Map<String, Long>> {
                val dailyTotals = calendarDays.associateWith { 0L }.toMutableMap()
                val appTotals = mutableMapOf<String, Long>()
                val dayWindows =
                    calendarDays.map { day ->
                        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
                        val dayEnd =
                            day
                                .plusDays(1)
                                .atStartOfDay(zone)
                                .toInstant()
                                .toEpochMilli()
                        DayWindow(day, dayStart, dayEnd)
                    }

                for ((pkgName, sessions) in sessionsByPackage) {
                    for (session in sessions) {
                        recordSessionOverlap(session, dayWindows, dailyTotals, appTotals, pkgName)
                    }
                }
                return dailyTotals to appTotals
            }

            private fun recordSessionOverlap(
                session: Pair<Long, Long>,
                dayWindows: List<DayWindow>,
                dailyTotals: MutableMap<LocalDate, Long>,
                appTotals: MutableMap<String, Long>,
                pkgName: String,
            ) {
                val (sessionStart, sessionEnd) = session
                for (window in dayWindows) {
                    val overlapStart = max(sessionStart, window.startMillis)
                    val overlapEnd = min(sessionEnd, window.endMillis)

                    if (overlapEnd > overlapStart) {
                        val durationMs = overlapEnd - overlapStart
                        dailyTotals[window.day] = (dailyTotals[window.day] ?: 0L) + durationMs
                        appTotals[pkgName] = (appTotals[pkgName] ?: 0L) + durationMs
                    }
                }
            }

            private data class DayWindow(
                val day: LocalDate,
                val startMillis: Long,
                val endMillis: Long,
            )

            private fun mergeIntervals(intervals: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
                if (intervals.isEmpty()) return emptyList()
                val sorted = intervals.sortedBy { it.first }
                val merged = mutableListOf<Pair<Long, Long>>()

                var current = sorted.first()
                for (i in 1 until sorted.size) {
                    val next = sorted[i]
                    current =
                        if (next.first <= current.second) {
                            current.first to max(current.second, next.second)
                        } else {
                            merged.add(current)
                            next
                        }
                }
                merged.add(current)
                return merged
            }
        }
    }
