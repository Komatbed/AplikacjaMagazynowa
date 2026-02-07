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
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.LocationDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After

class InventoryIntegrationTest {

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
        
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk<Operation>()

        repository = InventoryRepository(
            inventoryDao,
            pendingDao,
            auditLogDao,
            workManager
        ) { api }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `integration - refreshItems updates local DB from API`() = runTest {
        // Given
        val apiItem = InventoryItemDto(
            id = "api1",
            location = LocationDto(1, 1, 1, "API-LOC"),
            profileCode = "P_API",
            internalColor = "W",
            externalColor = "W",
            coreColor = "W",
            lengthMm = 2000,
            quantity = 50,
            status = "AVAILABLE"
        )
        coEvery { api.getItems(any(), any(), any(), any(), any()) } returns listOf(apiItem)
        
        // Mock DB behavior: after insert, the flow should emit the new item
        // But since we mock DAO, we simulate this by making the flow emit what we expect after "insert"
        // In a real integration test with Room, we would use an in-memory DB.
        // Here we verify the interaction flow: API -> Repo -> DAO Insert
        
        // When
        val result = repository.refreshItems()

        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            inventoryDao.insertAll(match { entities ->
                entities.size == 1 && 
                entities[0].id == "api1" &&
                entities[0].profileCode == "P_API"
            }) 
        }
    }
}
