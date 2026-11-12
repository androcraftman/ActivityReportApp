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
        if (sbn == null) return

        if (sbn.isOngoing) return
        val flags = sbn.notification?.flags ?: 0
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val key = sbn.key ?: return
        val packageName = sbn.packageName ?: return
        val postTime = sbn.postTime

        serviceScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    notificationKey = key,
                    packageName = packageName,
                    timestamp = postTime
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
