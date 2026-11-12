package com.aurendor.weeklyactivity.data.repository

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationManagerCompat
import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context
) : PermissionRepository {
    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        @Suppress("DEPRECATION")
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun hasNotificationAccess(): Boolean = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
}
