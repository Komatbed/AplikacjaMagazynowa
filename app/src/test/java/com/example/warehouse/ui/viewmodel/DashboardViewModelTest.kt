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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
import com.example.warehouse.ui.viewmodel.DashboardStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<InventoryRepository>(relaxed = true)
    
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // ViewModel creation moved to test to allow mock setup
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stats are default for empty items`() = runTest {
        every { repository.getItemsFlow() } returns flowOf(emptyList())

        viewModel = DashboardViewModel(application, repository)

        val results = mutableListOf<DashboardStats>()
        backgroundScope.launch {
            viewModel.stats.take(1).toList(results)
        }

        testDispatcher.scheduler.advanceUntilIdle()

        val stats = results.last()
        assertEquals(0, stats.totalItems)
        assertEquals(500, stats.freePalettes)
    }

    @Test
    fun `stats updates correctly based on items flow`() = runTest {
        // Given
        val mockItems = listOf(
            InventoryItemDto("id1", LocationDto(1, 1, 1, "loc1"), "P1", "White", "White", null, 6500, 10, "FULL"), // Full (length > 6000)
            InventoryItemDto("id2", LocationDto(1, 1, 1, "loc1"), "P1", "White", "White", null, 1000, 5, "WASTE"), // Waste
            InventoryItemDto("id3", LocationDto(1, 1, 1, "loc1"), "P2", "White", "White", null, 3000, 8, "AVAILABLE"),
            InventoryItemDto("id4", LocationDto(1, 1, 2, "loc2"), "P1", "White", "White", null, 3000, 2, "RESERVED") // Reserved
        )
        every { repository.getItemsFlow() } returns flowOf(mockItems)

        viewModel = DashboardViewModel(application, repository)

        // When & Then
        // We expect emissions.
        val results = mutableListOf<DashboardStats>()
        backgroundScope.launch {
            viewModel.stats.collect { 
                println("Collected: $it")
                results.add(it) 
            }
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        println("Results size: ${results.size}")
        
        // We expect the LAST item to be the correct one.
        // Even if we only get 1 item (if initial was skipped or fast-forwarded), it should be the calculated one?
        // No, initial is empty.
        
        val stats = results.last()
        println("Last stats: $stats")
        
        // Total items: 10 + 5 + 8 + 2 = 25
        assertEquals(25, stats.totalItems)
        
        // Waste count: status=WASTE (5) + length < 6000 (8 + 2) = 15
        assertEquals(15, stats.wasteCount)
        
        // Full count: status=FULL (10)
        assertEquals(10, stats.fullCount)
        
        // Reservation count: status=RESERVED (2)
        assertEquals(2, stats.reservationCount)
        
        // Unique profiles: P1, P2 -> 2
        assertEquals(2, stats.uniqueProfiles)
        
        // Occupied palettes: loc1 (1), loc2 (2) -> 2 distinct
        // Mock capacity 500. Free = 498
        assertEquals(498, stats.freePalettes)
    }
}
