package com.example.warehouse.service

import com.example.warehouse.dto.AlertLevel
import com.example.warehouse.dto.InventoryTakeRequest
import com.example.warehouse.dto.InventoryTakeResponse
import com.example.warehouse.dto.InventoryWasteRequest
import com.example.warehouse.dto.MapUpdateDTO
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.model.ItemStatus
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val locationRepository: LocationRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

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

        // Logic for Low Stock Warning (e.g. if taking this item drops below 5)
        // This is a simplified check. In real world, we might check global stock for this profile.
        if (!request.force && (targetItem.quantity - request.quantity) < 5) {
             return InventoryTakeResponse(
                 status = "WARNING",
                 newQuantity = targetItem.quantity,
                 warning = "Uwaga! Osiągnięto stan minimalny (poniżej 5). Potwierdź pobranie.",
                 code = "LOW_STOCK_WARNING"
             )
        }

        // Perform the take
        targetItem.quantity -= request.quantity
        inventoryItemRepository.save(targetItem)

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
    fun updateItemLength(id: UUID, newLength: Int): InventoryItem {
        val item = inventoryItemRepository.findById(id).orElseThrow {
            IllegalArgumentException("Nie znaleziono przedmiotu o ID: $id")
        }
        
        item.lengthMm = newLength
        return inventoryItemRepository.save(item)
    }

    private fun broadcastLocationUpdate(locationLabel: String) {
        // Recalculate stats for the location
        val items = inventoryItemRepository.findByLocation_Label(locationLabel)
        val totalItems = items.sumOf { it.quantity }
        
        // Logic for Occupancy & Alert
        // Assume simplified max capacity = 100 items for every location
        val maxCapacity = 100
        val occupancy = (totalItems * 100) / maxCapacity
        
        val alert = when {
            occupancy > 100 -> AlertLevel.CRITICAL
            occupancy > 80 -> AlertLevel.WARNING
            else -> AlertLevel.NONE
        }
        
        // Find dominant color (Mock logic, as InventoryItem doesn't store color directly yet, 
        // we'd normally join with ProfileType table. For now, we hash the profile code to get a color)
        val dominantColor = if (items.isNotEmpty()) {
            val mostFrequent = items.maxByOrNull { it.quantity }!!
            // Mock color based on profile code
            if (mostFrequent.profileCode.endsWith("W")) "#FFFFFF" else "#8B4513"
        } else "#888888"

        val update = MapUpdateDTO(
            locationLabel = locationLabel,
            occupancyPercentage = occupancy,
            itemsCount = totalItems,
            dominantColorHex = dominantColor,
            alertLevel = alert
        )
        
        messagingTemplate.convertAndSend("/topic/warehouse/map", update)
    }
}

