package com.example.warehouse.service

import com.example.warehouse.config.PalletConfig
import com.example.warehouse.config.WarehouseConfig
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.model.ItemStatus
import com.example.warehouse.model.Location
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import com.example.warehouse.repository.OperationLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.UUID

class InventoryServicePalletDetailsTest {

    @Test
    fun `getPalletDetails aggregates items and capacity`() {
        val inventoryRepo = Mockito.mock(InventoryItemRepository::class.java)
        val locationRepo = Mockito.mock(LocationRepository::class.java)
        val opLogRepo = Mockito.mock(OperationLogRepository::class.java)
        val messaging = Mockito.mock(SimpMessagingTemplate::class.java)
        val warehouseConfig = Mockito.mock(WarehouseConfig::class.java)
        val palletConfig = Mockito.mock(PalletConfig::class.java)

        Mockito.`when`(warehouseConfig.defaultPalletCapacity).thenReturn(50)

        val loc = Location(rowNumber = 1, paletteNumber = 1, label = "01A")
        Mockito.`when`(locationRepo.findByLabel("01A")).thenReturn(loc)

        val palletDef = PalletConfig.PalletDefinition(
            label = "01A",
            details = PalletConfig.PalletDetails(zone = "A", row = 1, type = "FULL_BARS"),
            capacity = 70,
            overflowThresholdPercent = 90,
            fillAnimation = "vertical",
            displayName = "01A",
            description = "Test"
        )
        Mockito.`when`(palletConfig.getPallet("01A")).thenReturn(palletDef)

        val item1 = InventoryItem(
            id = UUID.randomUUID(),
            location = loc,
            profileCode = "P1",
            internalColor = "W",
            externalColor = "W",
            coreColor = "701605",
            lengthMm = 6000,
            quantity = 10,
            status = ItemStatus.AVAILABLE
        )
        val item2 = InventoryItem(
            id = UUID.randomUUID(),
            location = loc,
            profileCode = "P2",
            internalColor = "W",
            externalColor = "W",
            coreColor = "701605",
            lengthMm = 500,
            quantity = 5,
            status = ItemStatus.RESERVED
        )
        Mockito.`when`(inventoryRepo.findByLocation_Label("01A")).thenReturn(listOf(item1, item2))

        val service = InventoryService(
            inventoryRepo,
            locationRepo,
            opLogRepo,
            messaging,
            warehouseConfig,
            palletConfig
        )

        val details = service.getPalletDetails("01A")

        assertEquals("01A", details.label)
        assertEquals("A", details.zone)
        assertEquals(1, details.row)
        assertEquals("FULL_BARS", details.type)
        assertEquals(70, details.capacity)
        assertEquals(15, details.totalItems)
        assertEquals(10, details.itemsAvailable)
        assertEquals(5, details.itemsReserved)
        assertEquals(0, details.itemsWaste)
        assertEquals(2, details.profiles.size)
    }
}

