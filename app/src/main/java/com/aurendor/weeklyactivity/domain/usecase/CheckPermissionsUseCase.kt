package com.aurendor.weeklyactivity.domain.usecase

import com.aurendor.weeklyactivity.domain.model.PermissionStatus
import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import javax.inject.Inject

class CheckPermissionsUseCase
    @Inject
    constructor(
        private val permissionRepository: PermissionRepository,
    ) {
        operator fun invoke(): PermissionStatus =
            PermissionStatus(
                hasUsageAccess = permissionRepository.hasUsageAccess(),
                hasNotificationAccess = permissionRepository.hasNotificationAccess(),
            )
    }
