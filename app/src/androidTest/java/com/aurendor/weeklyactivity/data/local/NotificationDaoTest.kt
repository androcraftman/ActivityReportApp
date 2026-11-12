package com.aurendor.weeklyactivity.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import com.aurendor.weeklyactivity.data.local.entity.NotificationEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var notificationDao: NotificationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        notificationDao = database.notificationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertDuplicateNotificationKeyIsIgnoredAndRangeQueryCountsAccurately() = runTest {
        val startTime = 1_000L
        val endTime = 2_000L

        notificationDao.insert(
            NotificationEntity(
                notificationKey = "key_chat_1",
                packageName = "com.whatsapp",
                timestamp = 1_500L
            )
        )

        // Duplicate key with later timestamp is ignored
        notificationDao.insert(
            NotificationEntity(
                notificationKey = "key_chat_1",
                packageName = "com.whatsapp",
                timestamp = 1_600L
            )
        )

        notificationDao.insert(
            NotificationEntity(
                notificationKey = "key_email_1",
                packageName = "com.google.android.gm",
                timestamp = 1_800L
            )
        )

        // Out of range (prior)
        notificationDao.insert(
            NotificationEntity(
                notificationKey = "key_old_1",
                packageName = "com.twitter.android",
                timestamp = 500L
            )
        )

        // Out of range (after)
        notificationDao.insert(
            NotificationEntity(
                notificationKey = "key_future_1",
                packageName = "com.twitter.android",
                timestamp = 2_500L
            )
        )

        val count = notificationDao.countBetween(startTime, endTime)

        assertEquals(2, count)
    }
}
