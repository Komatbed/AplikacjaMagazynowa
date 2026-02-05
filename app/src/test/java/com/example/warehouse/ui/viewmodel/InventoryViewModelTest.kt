package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<InventoryRepository>(relaxed = true)
    
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = InventoryViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadItems updates items from local DB flow`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto("id1", com.example.warehouse.data.model.LocationDto(1, 1, 1, "loc1"), "P1", 2000, 10, "AVAILABLE"),
            InventoryItemDto("id2", com.example.warehouse.data.model.LocationDto(1, 1, 1, "loc1"), "P1", 1500, 5, "AVAILABLE")
        )
        every { repository.getItemsFlow(any(), any()) } returns flowOf(mockItems)
        coEvery { repository.refreshItems(any(), any()) } returns Result.success(Unit)

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(mockItems, viewModel.items.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `loadItems shows error message when offline but keeps local data`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto("id1", com.example.warehouse.data.model.LocationDto(1, 1, 1, "loc1"), "P1", 2000, 10, "AVAILABLE")
        )
        every { repository.getItemsFlow(any(), any()) } returns flowOf(mockItems)
        coEvery { repository.refreshItems(any(), any()) } returns Result.failure(Exception("Network Error"))

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(mockItems, viewModel.items.value) // Should still have data
        assertEquals("Tryb offline: Wyświetlam lokalne dane", viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }
}
