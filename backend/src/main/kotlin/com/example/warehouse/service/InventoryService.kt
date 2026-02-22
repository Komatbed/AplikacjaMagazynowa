package com.example.warehouse.service

import com.example.warehouse.config.PalletConfig
import com.example.warehouse.config.WarehouseConfig
import com.example.warehouse.dto.AlertLevel
import com.example.warehouse.dto.InventoryReceiptRequest
import com.example.warehouse.dto.InventoryTakeRequest
import com.example.warehouse.dto.InventoryTakeResponse
import com.example.warehouse.dto.InventoryWasteRequest
import com.example.warehouse.dto.MapUpdateDTO
import com.example.warehouse.dto.PalletDetailsDto
import com.example.warehouse.dto.PalletSuggestionRequest
import com.example.warehouse.dto.PalletSuggestionResponse
import com.example.warehouse.controller.NotificationMessage
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.model.ItemStatus
import com.example.warehouse.model.OperationLog
import com.example.warehouse.model.User
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import com.example.warehouse.repository.OperationLogRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val locationRepository: LocationRepository,
    private val operationLogRepository: OperationLogRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    private val warehouseConfig: WarehouseConfig,
    private val palletConfig: PalletConfig
) {

    @Transactional
    fun registerReceipt(request: InventoryReceiptRequest): InventoryItem {
        val location = locationRepository.findByLabel(request.locationLabel)
            ?: throw IllegalArgumentException("Nie znaleziono lokalizacji: ${request.locationLabel}")

        // Check for existing similar item
        val existingItems = inventoryItemRepository.findByLocation_Label(request.locationLabel)
        val similarItem = existingItems.find { 
            it.profileCode == request.profileCode && 
            it.lengthMm == request.lengthMm &&
            it.internalColor == request.internalColor &&
            it.externalColor == request.externalColor &&
            it.coreColor == request.coreColor &&
            it.status == ItemStatus.AVAILABLE 
        }

        val result = if (similarItem != null) {
            similarItem.quantity += request.quantity
            inventoryItemRepository.save(similarItem)
        } else {
            val newItem = InventoryItem(
                id = UUID.randomUUID(),
                location = location,
                profileCode = request.profileCode,
                lengthMm = request.lengthMm,
                quantity = request.quantity,
                internalColor = request.internalColor,
                externalColor = request.externalColor,
                coreColor = request.coreColor,
                status = ItemStatus.AVAILABLE
            )
            inventoryItemRepository.save(newItem)
        }
        
        // Log Operation
        logOperation(
            type = "RECEIPT",
            item = result,
            quantityChange = request.quantity,
            reason = "PZ"
        )
        
        broadcastLocationUpdate(request.locationLabel)
        return result
    }

    @Transactional
    fun takeItem(request: InventoryTakeRequest): InventoryTakeResponse {
        val items = inventoryItemRepository.findByLocation_Label(request.locationLabel)
            .filter { it.lengthMm == request.lengthMm }
            
        // Filter by profile code if provided
        val targetItem = if (request.profileCode != null) {
            items.find { it.profileCode == request.profileCode }
        } else {
            items.firstOrNull()
        }

        if (targetItem == null) {
            throw IllegalArgumentException("Nie znaleziono przedmiotu o zadanych parametrach w lokalizacji ${request.locationLabel}")
        }

        if (targetItem.quantity < request.quantity) {
             throw IllegalArgumentException("Niewystarczająca ilość. Dostępne: ${targetItem.quantity}, Żądane: ${request.quantity}")
        }

        // Logic for Low Stock Warning (e.g. if taking this item drops below configured threshold)
        // This is a simplified check. In real world, we might check global stock for this profile.
        val limit = warehouseConfig.lowStockThreshold
        if (!request.force && (targetItem.quantity - request.quantity) < limit) {
             return InventoryTakeResponse(
                 status = "WARNING",
                 newQuantity = targetItem.quantity,
                 warning = "Uwaga! Osiągnięto stan minimalny (poniżej $limit). Potwierdź pobranie.",
                 code = "LOW_STOCK_WARNING"
             )
        }

        // Perform the take
        targetItem.quantity -= request.quantity
        inventoryItemRepository.save(targetItem)

        // Log Operation
        logOperation(
            type = "ISSUE",
            item = targetItem,
            quantityChange = -request.quantity,
            reason = "WZ"
        )

        // Broadcast update
        broadcastLocationUpdate(request.locationLabel)

        return InventoryTakeResponse(
            status = "SUCCESS",
            newQuantity = targetItem.quantity
        )
    }

    @Transactional
    fun registerWaste(request: InventoryWasteRequest): InventoryItem {
        val location = locationRepository.findByLabel(request.locationLabel)
            ?: throw IllegalArgumentException("Nie znaleziono lokalizacji: ${request.locationLabel}")

        // Check if similar waste item already exists in this location to merge
        val existingItems = inventoryItemRepository.findByLocation_Label(request.locationLabel)
        val similarItem = existingItems.find { 
            it.profileCode == request.profileCode && 
            it.lengthMm == request.lengthMm &&
            it.internalColor == request.internalColor &&
            it.externalColor == request.externalColor &&
            it.coreColor == request.coreColor &&
            it.status == ItemStatus.AVAILABLE 
        }

        val result = if (similarItem != null) {
            similarItem.quantity += request.quantity
            inventoryItemRepository.save(similarItem)
        } else {
            val newItem = InventoryItem(
                id = UUID.randomUUID(),
                location = location,
                profileCode = request.profileCode,
                lengthMm = request.lengthMm,
                quantity = request.quantity,
                internalColor = request.internalColor,
                externalColor = request.externalColor,
                coreColor = request.coreColor,
                status = ItemStatus.AVAILABLE
            )
            inventoryItemRepository.save(newItem)
        }
        
        // Broadcast update
        broadcastLocationUpdate(request.locationLabel)
        
        return result
    }

    @Transactional
    fun deleteItem(id: UUID) {
        val optional = inventoryItemRepository.findById(id)
        if (optional.isEmpty) {
            return
        }
        val item = optional.get()
        val locationLabel = item.location.label
        val quantity = item.quantity

        inventoryItemRepository.delete(item)

        val authentication = SecurityContextHolder.getContext().authentication
        val userId = if (authentication != null && authentication.principal is User) {
            (authentication.principal as User).id
        } else {
            null
        }

        val log = OperationLog(
            operationType = "DELETE",
            inventoryItem = null,
            location = item.location,
            quantityChange = -quantity,
            reason = "Usunięcie pozycji z magazynu",
            userId = userId,
            timestamp = java.time.LocalDateTime.now()
        )
        operationLogRepository.save(log)

        broadcastLocationUpdate(locationLabel)
    }

    @Transactional(readOnly = true)
    fun getPalletDetails(label: String): PalletDetailsDto {
        val items = inventoryItemRepository.findByLocation_Label(label)
        val totalItems = items.sumOf { it.quantity }

        val palletDefinition = palletConfig.getPallet(label)
        val configuredCapacity = palletDefinition?.capacity

        val location = locationRepository.findByLabel(label)
        val locationCapacity = location?.capacity

        val maxCapacity = when {
            configuredCapacity != null && configuredCapacity > 0 -> configuredCapacity
            locationCapacity != null && locationCapacity > 0 -> locationCapacity
            else -> warehouseConfig.defaultPalletCapacity
        }

        val occupancy = if (maxCapacity > 0) {
            (totalItems * 100) / maxCapacity
        } else {
            0
        }

        val available = items.filter { it.status == ItemStatus.AVAILABLE }.sumOf { it.quantity }
        val reserved = items.filter { it.status == ItemStatus.RESERVED }.sumOf { it.quantity }
        val waste = items.filter { it.status == ItemStatus.WASTE }.sumOf { it.quantity }

        val profiles = items.map { it.profileCode }.distinct().sorted().take(5)
        val coreColors = items.mapNotNull { it.coreColor }.filter { it.isNotEmpty() }.distinct().sorted().take(5)

        return PalletDetailsDto(
            label = label,
            zone = palletDefinition?.details?.zone,
            row = palletDefinition?.details?.row,
            type = palletDefinition?.details?.type,
            capacity = maxCapacity,
            occupancyPercentage = occupancy,
            totalItems = totalItems,
            itemsAvailable = available,
            itemsReserved = reserved,
            itemsWaste = waste,
            profiles = profiles,
            coreColors = coreColors
        )
    }

    @Transactional(readOnly = true)
    fun suggestPallet(request: PalletSuggestionRequest): PalletSuggestionResponse {
        val label = palletConfig.suggestPalletLabel(
            profileCode = request.profileCode,
            internalColor = request.internalColor,
            externalColor = request.externalColor,
            coreColor = request.coreColor,
            isWaste = request.isWaste
        )
        return PalletSuggestionResponse(label = label)
    }
    
    @Transactional
    fun updateItemLength(id: UUID, newLength: Int): InventoryItem {
        val item = inventoryItemRepository.findById(id).orElseThrow {
            IllegalArgumentException("Nie znaleziono przedmiotu o ID: $id")
        }
        
        item.lengthMm = newLength
        return inventoryItemRepository.save(item)
    }

    @Transactional
    fun reserveItem(request: com.example.warehouse.dto.ReservationRequest): InventoryItem {
        val item = inventoryItemRepository.findById(request.itemId).orElseThrow {
            IllegalArgumentException("Nie znaleziono przedmiotu o ID: ${request.itemId}")
        }

        if (item.status != ItemStatus.AVAILABLE) {
             throw IllegalStateException("Przedmiot nie jest dostępny (Status: ${item.status})")
        }

        if (request.quantity > item.quantity) {
             throw IllegalArgumentException("Niewystarczająca ilość do rezerwacji. Dostępne: ${item.quantity}")
        }

        // If reserving partial quantity
        if (request.quantity < item.quantity) {
             // Reduce original item quantity
             item.quantity -= request.quantity
             inventoryItemRepository.save(item)

             // Create new item for reservation
             val reservedItem = item.copy(
                 id = UUID.randomUUID(),
                 quantity = request.quantity,
                 status = ItemStatus.RESERVED,
                 reservedBy = request.reservedBy,
                 reservationDate = java.time.LocalDateTime.now(),
                 createdAt = java.time.LocalDateTime.now(),
                 updatedAt = java.time.LocalDateTime.now()
             )
             val savedReserved = inventoryItemRepository.save(reservedItem)
             
             logOperation("RESERVATION", savedReserved, request.quantity, "Rezerwacja dla ${request.reservedBy} ${request.notes ?: ""}")
             broadcastLocationUpdate(item.location.label)
             return savedReserved
        } else {
             // Reserving full quantity
             item.status = ItemStatus.RESERVED
             item.reservedBy = request.reservedBy
             item.reservationDate = java.time.LocalDateTime.now()
             item.updatedAt = java.time.LocalDateTime.now()
             val savedItem = inventoryItemRepository.save(item)
             
             logOperation("RESERVATION", savedItem, 0, "Rezerwacja całości dla ${request.reservedBy} ${request.notes ?: ""}")
             broadcastLocationUpdate(item.location.label)
             return savedItem
        }
    }

    @Transactional
    fun cancelReservation(request: com.example.warehouse.dto.ReservationCancelRequest): InventoryItem {
         val item = inventoryItemRepository.findById(request.itemId).orElseThrow {
            IllegalArgumentException("Nie znaleziono przedmiotu o ID: ${request.itemId}")
        }

        if (item.status != ItemStatus.RESERVED) {
             throw IllegalStateException("Przedmiot nie jest zarezerwowany.")
        }

        // Revert status
        item.status = ItemStatus.AVAILABLE
        item.reservedBy = null
        item.reservationDate = null
        item.updatedAt = java.time.LocalDateTime.now()
        
        val savedItem = inventoryItemRepository.save(item)
        
        logOperation("RESERVATION_CANCEL", savedItem, 0, "Anulowanie rezerwacji")
        broadcastLocationUpdate(item.location.label)
        return savedItem
    }

    @Transactional
    fun completeReservation(request: com.example.warehouse.dto.ReservationCancelRequest): InventoryTakeResponse {
         val item = inventoryItemRepository.findById(request.itemId).orElseThrow {
            IllegalArgumentException("Nie znaleziono przedmiotu o ID: ${request.itemId}")
        }

        if (item.status != ItemStatus.RESERVED) {
             throw IllegalStateException("Przedmiot nie jest zarezerwowany.")
        }
        
        val qty = item.quantity
        val reservedBy = item.reservedBy
        
        // Delete the item as it is taken
        inventoryItemRepository.delete(item)
        
        // We need to create a dummy item for logging or just log properties
        // But logOperation takes InventoryItem. 
        // We can pass the deleted item object (it still exists in memory)
        logOperation("ISSUE", item, -qty, "Realizacja rezerwacji dla $reservedBy")
        broadcastLocationUpdate(item.location.label)
        
        return InventoryTakeResponse(status="SUCCESS", newQuantity=0)
    }

    private fun logOperation(type: String, item: InventoryItem, quantityChange: Int, reason: String?) {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = if (authentication != null && authentication.principal is User) {
            (authentication.principal as User).id
        } else {
            null
        }

        val log = OperationLog(
            operationType = type,
            inventoryItem = item,
            location = item.location,
            quantityChange = quantityChange,
            reason = reason,
            userId = userId,
            timestamp = java.time.LocalDateTime.now()
        )
        operationLogRepository.save(log)
    }

    private fun broadcastLocationUpdate(locationLabel: String) {
        val items = inventoryItemRepository.findByLocation_Label(locationLabel)
        val totalItems = items.sumOf { it.quantity }

        val palletDefinition = palletConfig.getPallet(locationLabel)
        val configuredCapacity = palletDefinition?.capacity

        val location = locationRepository.findByLabel(locationLabel)
        val locationCapacity = location?.capacity

        val maxCapacity = when {
            configuredCapacity != null && configuredCapacity > 0 -> configuredCapacity
            locationCapacity != null && locationCapacity > 0 -> locationCapacity
            else -> warehouseConfig.defaultPalletCapacity
        }

        val occupancy = if (maxCapacity > 0) {
            (totalItems * 100) / maxCapacity
        } else {
            0
        }

        val overflowThreshold = palletDefinition?.overflowThresholdPercent ?: 100

        val alert = when {
            occupancy > 100 -> AlertLevel.CRITICAL
            occupancy >= overflowThreshold && overflowThreshold < 100 -> AlertLevel.WARNING
            else -> AlertLevel.NONE
        }

        val dominantColor = if (items.isNotEmpty()) {
            val groupedByColor = items
                .groupBy { it.coreColor?.lowercase().orEmpty() }
                .filterKeys { it.isNotEmpty() }

            if (groupedByColor.isNotEmpty()) {
                val (colorCode, colorItems) = groupedByColor.maxByOrNull { entry ->
                    entry.value.sumOf { it.quantity }
                }!!
                "#${colorCode}"
            } else {
                "#888888"
            }
        } else {
            "#888888"
        }

        val update = MapUpdateDTO(
            locationLabel = locationLabel,
            occupancyPercentage = occupancy,
            itemsCount = totalItems,
            dominantColorHex = dominantColor,
            alertLevel = alert
        )

        messagingTemplate.convertAndSend("/topic/warehouse/map", update)

        if (alert == AlertLevel.CRITICAL || alert == AlertLevel.WARNING) {
            val msg = NotificationMessage(
                title = "Alert Magazynowy: $locationLabel",
                message = "Poziom zajętości: $occupancy%. Wymagana uwaga.",
                type = if (alert == AlertLevel.CRITICAL) "ERROR" else "WARNING"
            )
            messagingTemplate.convertAndSend("/topic/notifications", msg)
        }
    }
}
