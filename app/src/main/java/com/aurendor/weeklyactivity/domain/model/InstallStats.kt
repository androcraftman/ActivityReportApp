package com.aurendor.weeklyactivity.domain.model

data class InstallStats(
    val installedCount: Int,
    val removedCount: Int,
) {
    val totalEvents: Int get() = installedCount + removedCount
}
