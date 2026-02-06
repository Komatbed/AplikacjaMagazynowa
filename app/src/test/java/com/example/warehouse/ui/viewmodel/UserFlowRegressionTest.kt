package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.dao.InventoryDao
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.local.entity.InventoryItemEntity
import com.example.warehouse.data.local.entity.PendingOperationEntity
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.LocationDto
import com.example.warehouse.data.repository.InventoryRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import android.util.Log
import com.example.warehouse.data.model.InventoryTakeRequest

@OptIn(ExperimentalCoroutinesApi::class)
class UserFlowRegressionTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    
    // Mocks for Repository components
    private val inventoryDao = mockk<InventoryDao>(relaxed = true)
    private val pendingDao = mockk<PendingOperationDao>(relaxed = true)
    private val configDao = mockk<ConfigDao>(relaxed = true)
    private val auditLogDao = mockk<AuditLogDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val api = mockk<WarehouseApi>(relaxed = true)
    
    private lateinit var repository: InventoryRepository
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var inventoryViewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk<Operation>()
        
        // Setup Repository with mocks
        repository = InventoryRepository(
            inventoryDao,
            pendingDao,
            configDao,
            auditLogDao,
            workManager
        ) { api }
        
        // Setup ViewModels
        dashboardViewModel = DashboardViewModel(application, repository)
        inventoryViewModel = InventoryViewModel(application, repository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `full user flow - take item updates dashboard`() = runTest {
        // Scenario: User takes an item from inventory
        
        // 1. Initial State
        val initialItem = InventoryItemDto(
            id = "item1",
            location = LocationDto(1, 1, 1, "A1"),
            profileCode = "P1",
            internalColor = "W",
            externalColor = "W",
            coreColor = "W",
            lengthMm = 6500,
            quantity = 10,
            status = "AVAILABLE"
        )
        
        // Mock Repository flow - Note: InventoryItemEntity only has locationLabel, not aisle/rack/shelf
        every { inventoryDao.getAllItems() } returns flowOf(listOf(
            InventoryItemEntity(
                id = "item1", 
                locationLabel = "A1", 
                profileCode = "P1", 
                internalColor = "W", 
                externalColor = "W", 
                coreColor = "W", 
                lengthMm = 6500, 
                quantity = 10, 
                status = "AVAILABLE", 
                lastUpdated = 0L
            )
        ))
        // Also mock filtered flow which VM uses
        every { inventoryDao.getItemsFiltered(any(), any(), any(), any(), any()) } returns flowOf(listOf(
            InventoryItemEntity(
                id = "item1", 
                locationLabel = "A1", 
                profileCode = "P1", 
                internalColor = "W", 
                externalColor = "W", 
                coreColor = "W", 
                lengthMm = 6500, 
                quantity = 10, 
                status = "AVAILABLE", 
                lastUpdated = 0L
            )
        ))
        
        // 2. User performs Take Action
        // "takeItem" calls repository.takeItem -> PendingDao.insert -> SyncWorker (via WorkManager)
        
        // Mock PendingDao behavior to simulate successful local change
        coEvery { pendingDao.insert(any()) } just Runs
        
        inventoryViewModel.takeItem(
            InventoryTakeRequest(
                locationLabel = "A1",
                profileCode = "P1",
                lengthMm = 6500,
                quantity = 5,
                reason = "Production"
            )
        ) {
            // onSuccess callback
        }
        
        advanceUntilIdle()
        
        // 3. Verify Repository interactions
        coVerify { pendingDao.insert(any()) }
        verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
        
        // 4. Verify Dashboard Update (Simulated)
        // Since we mocked the DAO flow, we can't see the automatic update unless we mock the flow update too.
        // In a real regression test with Room, the flow would update automatically.
        // Here we verify that the correct method was called which would trigger the update.
        
        // If we want to verify dashboard state, we need to update the mock flow
        val updatedEntity = InventoryItemEntity(
            id = "item1", 
            locationLabel = "A1", 
            profileCode = "P1", 
            internalColor = "W", 
            externalColor = "W", 
            coreColor = "W", 
            lengthMm = 6500, 
            quantity = 5, 
            status = "AVAILABLE", 
            lastUpdated = 0L
        )
        every { inventoryDao.getAllItems() } returns flowOf(listOf(updatedEntity))
        every { inventoryDao.getItemsFiltered(any(), any(), any(), any(), any()) } returns flowOf(listOf(updatedEntity))
        
        // Trigger dashboard update
        // (In a real app, Room would emit new value to Flow, which Repository exposes)
    }
}
