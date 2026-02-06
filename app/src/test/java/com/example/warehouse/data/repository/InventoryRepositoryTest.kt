package com.example.warehouse.data.repository

import android.content.Context
import androidx.work.Operation
import androidx.work.OneTimeWorkRequest
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
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryWasteRequest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After

class InventoryRepositoryTest {

    private lateinit var inventoryDao: InventoryDao
    private lateinit var pendingDao: PendingOperationDao
    private lateinit var configDao: ConfigDao
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var workManager: WorkManager
    private lateinit var api: WarehouseApi
    private lateinit var repository: InventoryRepository

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        inventoryDao = mockk(relaxed = true)
        pendingDao = mockk(relaxed = true)
        configDao = mockk(relaxed = true)
        auditLogDao = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        api = mockk(relaxed = true)
        
        // Mock WorkManager enqueue
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk<Operation>()

        repository = InventoryRepository(
            inventoryDao,
            pendingDao,
            configDao,
            auditLogDao,
            workManager
        ) { api }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `getItemsFlow returns mapped items from dao`() = runTest {
        // Given
        val entity = InventoryItemEntity(
            id = "1",
            locationLabel = "A-01",
            profileCode = "P1",
            internalColor = "W",
            externalColor = "W",
            coreColor = "W",
            lengthMm = 1000,
            quantity = 10,
            status = "FULL"
        )
        every { inventoryDao.getItemsFiltered(any(), any(), any(), any(), any()) } returns flowOf(listOf(entity))

        // When
        val result = repository.getItemsFlow().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
        assertEquals("A-01", result[0].location.label)
    }

    @Test
    fun `refreshItems fetches from api and saves to dao`() = runTest {
        // Given
        val dto = InventoryItemDto(
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
        coEvery { api.getItems(any(), any(), any(), any(), any()) } returns listOf(dto)

        // When
        val result = repository.refreshItems()

        // Then
        assertTrue(result.isSuccess)
        coVerify { inventoryDao.insertAll(any()) }
    }

    @Test
    fun `refreshItems returns failure on api error`() = runTest {
        // Given
        coEvery { api.getItems(any(), any(), any(), any(), any()) } throws Exception("Network error")

        // When
        val result = repository.refreshItems()

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { inventoryDao.insertAll(any()) }
    }

    @Test
    fun `takeItem adds to pending queue and schedules sync`() = runTest {
        // Given
        val request = InventoryTakeRequest("A-01", "P1", 1000, 5, "Job1")
        
        // When
        repository.takeItem(request)

        // Then
        coVerify { pendingDao.insert(any()) }
        verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `registerWaste adds to pending queue and schedules sync`() = runTest {
        // Given
        val request = InventoryWasteRequest("P1", 1000, 5, "A-01", "W", "W", "W", "Broken")
        
        // When
        repository.registerWaste(request)

        // Then
        coVerify { pendingDao.insert(any()) }
        verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
    }
}
