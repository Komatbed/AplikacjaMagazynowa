package com.example.warehouse.data.repository

import androidx.work.Operation
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.dao.InventoryDao
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.model.PalletDetailsDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After

class PalletDetailsRepositoryTest {

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
    fun `getPalletDetails returns success from api`() = kotlinx.coroutines.test.runTest {
        val dto = PalletDetailsDto(
            label = "01A",
            zone = "A",
            row = 1,
            type = "FULL_BARS",
            capacity = 70,
            occupancyPercentage = 50,
            totalItems = 35,
            itemsAvailable = 20,
            itemsReserved = 10,
            itemsWaste = 5,
            profiles = listOf("P1", "P2"),
            coreColors = listOf("701605")
        )
        coEvery { api.getPalletDetails("01A") } returns dto

        val result = repository.getPalletDetails("01A")

        assertTrue(result.isSuccess)
        assertEquals("01A", result.getOrNull()?.label)
        coVerify { api.getPalletDetails("01A") }
    }
}

