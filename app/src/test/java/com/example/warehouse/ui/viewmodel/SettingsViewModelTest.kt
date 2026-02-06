package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.warehouse.data.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<InventoryRepository>(relaxed = true)
    
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkBackendConnection updates status to Online on success`() = runTest {
        // Given
        coEvery { repository.checkConnection() } returns Result.success(Unit)

        // When
        viewModel.checkBackendConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val status = viewModel.backendStatus.value
        assertTrue(status is BackendStatus.Online)
    }

    @Test
    fun `checkBackendConnection updates status to Offline on failure`() = runTest {
        // Given
        coEvery { repository.checkConnection() } returns Result.failure(Exception("Timeout"))

        // When
        viewModel.checkBackendConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val status = viewModel.backendStatus.value
        assertTrue(status is BackendStatus.Offline)
    }
}
