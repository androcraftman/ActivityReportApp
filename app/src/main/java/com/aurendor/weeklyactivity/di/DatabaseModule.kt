package com.aurendor.weeklyactivity.di

import android.content.Context
import androidx.room.Room
import com.aurendor.weeklyactivity.data.local.AppDatabase
import com.aurendor.weeklyactivity.data.local.dao.AppEventDao
import com.aurendor.weeklyactivity.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            ).build()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideAppEventDao(database: AppDatabase): AppEventDao = database.appEventDao()
}
