package com.aurendor.weeklyactivity.di

import com.aurendor.weeklyactivity.data.repository.ActivityReportRepositoryImpl
import com.aurendor.weeklyactivity.data.repository.PermissionRepositoryImpl
import com.aurendor.weeklyactivity.domain.repository.ActivityReportRepository
import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        impl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindActivityReportRepository(
        impl: ActivityReportRepositoryImpl
    ): ActivityReportRepository
}
