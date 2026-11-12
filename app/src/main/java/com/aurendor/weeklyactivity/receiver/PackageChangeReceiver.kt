package com.aurendor.weeklyactivity.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aurendor.weeklyactivity.data.local.dao.AppEventDao
import com.aurendor.weeklyactivity.data.local.entity.AppEventEntity
import com.aurendor.weeklyactivity.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {
    @Inject
    lateinit var appEventDao: AppEventDao

    @Inject
    lateinit var clock: Clock

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_FULLY_REMOVED) return

        val packageName = intent.data?.schemeSpecificPart ?: return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        scope.launch {
            try {
                appEventDao.insert(
                    AppEventEntity(
                        packageName = packageName,
                        eventType = AppEventEntity.TYPE_REMOVED,
                        timestamp = clock.millis()
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
