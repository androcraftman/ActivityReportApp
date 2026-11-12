package com.aurendor.weeklyactivity.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import com.aurendor.weeklyactivity.data.local.entity.NotificationEntity
import com.aurendor.weeklyactivity.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationDao: NotificationDao

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val serviceScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || !shouldTrack(sbn)) return

        serviceScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    timestamp = sbn.postTime
                )
            )
        }
    }

    private fun shouldTrack(sbn: StatusBarNotification): Boolean =
        !sbn.isOngoing && (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
