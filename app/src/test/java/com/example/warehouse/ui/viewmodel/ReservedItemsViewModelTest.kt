package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.LocationDto
import com.example.warehouse.data.repository.InventoryRepository
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
class ReservedItemsViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<InventoryRepository>(relaxed = true)
    
    private lateinit var viewModel: ReservedItemsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ReservedItemsViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadItems filters reserved and in_progress items`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto("id1", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "AVAILABLE"),
            InventoryItemDto("id2", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "RESERVED", "UserA", "2024-05-20"),
            InventoryItemDto("id3", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "IN_PROGRESS", "UserB", "2024-05-21"),
            InventoryItemDto("id4", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "WASTE")
        )
        every { repository.getItemsFlow() } returns flowOf(mockItems)

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val items = viewModel.items.value
        assertEquals(2, items.size)
        assertEquals("RESERVED", items[0].status)
        assertEquals("IN_PROGRESS", items[1].status)
    }

    @Test
    fun `filtering by user updates items list`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto("id1", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "RESERVED", "UserA", "2024-05-20"),
            InventoryItemDto("id2", LocationDto(1, 1, 1, "loc1"), "P1", "W", "W", null, 2000, 5, "IN_PROGRESS", "UserB", "2024-05-21")
        )
        every { repository.getItemsFlow() } returns flowOf(mockItems)

        // Initial load
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.items.value.size)

        // When filtering by UserA
        viewModel.updateFilterUser("UserA")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.items.value.size)
        assertEquals("UserA", viewModel.items.value[0].reservedBy)
    }
}
