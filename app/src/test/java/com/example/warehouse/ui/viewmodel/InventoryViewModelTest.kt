package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.data.model.LocationDto
import com.example.warehouse.data.repository.ConfigRepository
import com.example.warehouse.data.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InventoryRepository
    private lateinit var configRepository: ConfigRepository
    private lateinit var application: Application
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        repository = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)
        application = mockk(relaxed = true)

        // Mock StateFlow sources
        every { configRepository.getProfilesFlow() } returns flowOf(emptyList())
        every { configRepository.getColorsFlow() } returns flowOf(emptyList())
        coEvery { configRepository.refreshConfig() } returns Result.success(Unit)

        viewModel = InventoryViewModel(application, repository, configRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `loadItems updates items state from repository flow`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto(
                id = "1",
                location = LocationDto(1, 1, 1, "A-01"),
                profileCode = "P1",
                internalColor = "W",
                externalColor = "W",
                coreColor = "W",
                lengthMm = 1000,
                quantity = 10,
                status = "FULL"
            )
        )
        every { repository.getItemsFlow(any(), any(), any(), any(), any()) } returns flowOf(mockItems)
        coEvery { repository.refreshItems(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.items.value.size)
        assertEquals("P1", viewModel.items.value[0].profileCode)
    }

    @Test
    fun `loadItems calls refreshItems`() = runTest {
        // Given
        every { repository.getItemsFlow(any(), any(), any(), any(), any()) } returns flowOf(emptyList())
        coEvery { repository.refreshItems(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        // When
        viewModel.loadItems(location = "A-01")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.refreshItems(location = "A-01", any(), any(), any(), any()) }
    }

    @Test
    fun `takeItem calls repository and executes callback`() = runTest {
        // Given
        val request = InventoryTakeRequest("A-01", "P1", 1000, 5, "Job1")
        var callbackExecuted = false
        coEvery { repository.takeItem(request) } returns Unit

        // When
        viewModel.takeItem(request) { callbackExecuted = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.takeItem(request) }
        assertEquals(true, callbackExecuted)
    }

    @Test
    fun `registerWaste calls repository and executes callback`() = runTest {
        // Given
        val request = InventoryWasteRequest("P1", 1000, 5, "A-01", "W", "W", "W", "Broken")
        var callbackExecuted = false
        coEvery { repository.registerWaste(request) } returns Unit

        // When
        viewModel.registerWaste(request) { callbackExecuted = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.registerWaste(request) }
        assertEquals(true, callbackExecuted)
    }

    @Test
    fun `updateItemLength calls repository and updates local state`() = runTest {
        // Given
        val item = InventoryItemDto(
            id = "1",
            location = LocationDto(1, 1, 1, "A-01"),
            profileCode = "P1",
            internalColor = "W",
            externalColor = "W",
            coreColor = "W",
            lengthMm = 1000,
            quantity = 1,
            status = "FULL"
        )
        // Setup initial state
        every { repository.getItemsFlow(any(), any(), any(), any(), any()) } returns flowOf(listOf(item))
        coEvery { repository.refreshItems(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { repository.updateItemLength(any(), any()) } returns Unit

        // When
        viewModel.updateItemLength(item, 2000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.updateItemLength(item, 2000) }
        val updatedItem = viewModel.items.value.find { it.id == "1" }
        assertEquals(2000, updatedItem?.lengthMm)
    }

    @Test
    fun `loadItems sets error message on failure`() = runTest {
        // Given
        every { repository.getItemsFlow(any(), any(), any(), any(), any()) } returns flowOf(emptyList())
        coEvery { repository.refreshItems(any(), any(), any(), any(), any()) } returns Result.failure(Exception("Network Error"))
        coEvery { configRepository.refreshConfig() } returns Result.success(Unit)

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Tryb offline: Wyświetlam lokalne dane", viewModel.error.value)
    }
}
