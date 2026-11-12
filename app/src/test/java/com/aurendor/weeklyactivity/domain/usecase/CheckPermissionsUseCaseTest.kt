package com.aurendor.weeklyactivity.domain.usecase

import com.aurendor.weeklyactivity.domain.repository.PermissionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckPermissionsUseCaseTest {
    private val permissionRepository: PermissionRepository = mockk()
    private val useCase = CheckPermissionsUseCase(permissionRepository)

    @Test
    fun `invoke returns correct status when both permissions granted`() {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns true

        val status = useCase()

        assertTrue(status.hasUsageAccess)
        assertTrue(status.hasNotificationAccess)
        assertTrue(status.isFullyGranted)
    }

    @Test
    fun `invoke returns correct status when only usage permission granted`() {
        every { permissionRepository.hasUsageAccess() } returns true
        every { permissionRepository.hasNotificationAccess() } returns false

        val status = useCase()

        assertTrue(status.hasUsageAccess)
        assertFalse(status.hasNotificationAccess)
        assertFalse(status.isFullyGranted)
    }

    @Test
    fun `invoke returns correct status when only notification permission granted`() {
        every { permissionRepository.hasUsageAccess() } returns false
        every { permissionRepository.hasNotificationAccess() } returns true

        val status = useCase()

        assertFalse(status.hasUsageAccess)
        assertTrue(status.hasNotificationAccess)
        assertFalse(status.isFullyGranted)
    }

    @Test
    fun `invoke returns correct status when both permissions denied`() {
        every { permissionRepository.hasUsageAccess() } returns false
        every { permissionRepository.hasNotificationAccess() } returns false

        val status = useCase()

        assertFalse(status.hasUsageAccess)
        assertFalse(status.hasNotificationAccess)
        assertFalse(status.isFullyGranted)
    }
}
