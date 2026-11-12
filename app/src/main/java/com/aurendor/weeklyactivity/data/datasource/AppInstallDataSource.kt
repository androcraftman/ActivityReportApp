package com.aurendor.weeklyactivity.data.datasource

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aurendor.weeklyactivity.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AppInstallDataSource
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledCount(startTime: Instant): Int = withContext(ioDispatcher) {
        val launchIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

        val launchablePackages =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launchIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launchIntent, 0)
            }.map { it.activityInfo.packageName }.toSet()

        val allPackages =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }

        val startMillis = startTime.toEpochMilli()
        allPackages.count { packageInfo ->
            packageInfo.packageName != context.packageName &&
                packageInfo.packageName in launchablePackages &&
                packageInfo.firstInstallTime >= startMillis
        }
    }

    suspend fun getAppLabel(packageName: String): String = withContext(ioDispatcher) {
        try {
            val appInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(packageName, 0)
                }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
