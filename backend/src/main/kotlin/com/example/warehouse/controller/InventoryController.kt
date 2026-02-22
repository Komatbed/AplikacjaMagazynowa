package com.example.warehouse.controller

import com.example.warehouse.dto.InventoryReceiptRequest
import com.example.warehouse.dto.InventoryTakeRequest
import com.example.warehouse.dto.InventoryTakeResponse
import com.example.warehouse.dto.InventoryWasteRequest
import com.example.warehouse.dto.PalletSuggestionRequest
import com.example.warehouse.dto.PalletSuggestionResponse
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.service.InventoryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryService: InventoryService,
    private val profileDefinitionRepository: com.example.warehouse.repository.ProfileDefinitionRepository,
    private val colorDefinitionRepository: com.example.warehouse.repository.ColorDefinitionRepository,
    private val operationLogRepository: com.example.warehouse.repository.OperationLogRepository
) {
    @GetMapping("/config")
    fun getConfig(): Map<String, List<String>> {
        // Return defined profiles and colors from Master Data
        val profiles = profileDefinitionRepository.findAll().map { it.code }.sorted()
        val colors = colorDefinitionRepository.findAll().map { it.code }.sorted()
        
        // Fallback: If master data is empty (shouldn't be due to seed), use existing items
        if (profiles.isEmpty() && colors.isEmpty()) {
             val itemProfiles = inventoryItemRepository.findAll().map { it.profileCode }.distinct().sorted()
             val itemColors = inventoryItemRepository.findAll().flatMap { listOf(it.internalColor, it.externalColor, it.coreColor ?: "") }
                 .filter { it.isNotEmpty() }
                 .distinct()
                 .sorted()
             return mapOf(
                "profiles" to itemProfiles,
                "colors" to itemColors
            )
        }

        return mapOf(
            "profiles" to profiles,
            "colors" to colors
        )
    }

    @GetMapping("/items")
    fun getItems(
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) profileCode: String?,
        @RequestParam(required = false) internalColor: String?,
        @RequestParam(required = false) externalColor: String?,
        @RequestParam(required = false) coreColor: String?,
        @RequestParam(required = false) status: com.example.warehouse.model.ItemStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<InventoryItem> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        return inventoryItemRepository
            .findFiltered(location, profileCode, internalColor, externalColor, coreColor, status, pageable)
            .content
    }

    @PostMapping("/take")
    fun takeItem(@Valid @RequestBody request: InventoryTakeRequest): InventoryTakeResponse {
        return inventoryService.takeItem(request)
    }

    @PostMapping("/waste")
    fun registerWaste(@Valid @RequestBody request: InventoryWasteRequest): InventoryItem {
        return inventoryService.registerWaste(request)
    }

    // New endpoints to match api.js
    // api.js: postReceipt(data) -> /inventory/receipt
    // Maps to existing logic or new logic for external receipt
    @PostMapping("/receipt")
    fun registerReceipt(@Valid @RequestBody request: InventoryReceiptRequest): InventoryItem {
         // Assuming receipt logic is similar to registering waste (adding new item) but for standard stock
         // Or map to a new service method
         // For now, let's assume it uses similar logic to registerWaste but marks as AVAILABLE
         // We need to implement registerReceipt in InventoryService
         return inventoryService.registerReceipt(request)
    }

    @PostMapping("/suggest-location")
    fun suggestLocation(@Valid @RequestBody request: PalletSuggestionRequest): PalletSuggestionResponse {
        return inventoryService.suggestPallet(request)
    }

    // api.js: postIssue(data) -> /inventory/issue
    // Maps to takeItem logic
    @PostMapping("/issue")
    fun registerIssue(@Valid @RequestBody request: InventoryTakeRequest): InventoryTakeResponse {
        return inventoryService.takeItem(request)
    }

    @PostMapping("/reserve")
    fun reserveItem(@Valid @RequestBody request: com.example.warehouse.dto.ReservationRequest): InventoryItem {
        return inventoryService.reserveItem(request)
    }

    @GetMapping("/pallet/{label}")
    fun getPalletDetails(@PathVariable label: String): com.example.warehouse.dto.PalletDetailsDto {
        return inventoryService.getPalletDetails(label)
    }


    @PostMapping("/reserve/cancel")
    fun cancelReservation(@Valid @RequestBody request: com.example.warehouse.dto.ReservationCancelRequest): InventoryItem {
        return inventoryService.cancelReservation(request)
    }

    @PostMapping("/reserve/complete")
    fun completeReservation(@Valid @RequestBody request: com.example.warehouse.dto.ReservationCancelRequest): InventoryTakeResponse {
        return inventoryService.completeReservation(request)
    }

    @PutMapping("/items/{id}/length")
    fun updateItemLength(@PathVariable id: UUID, @RequestBody length: Int): InventoryItem {
        return inventoryService.updateItemLength(id, length)
    }

    @DeleteMapping("/items/{id}")
    fun deleteItem(@PathVariable id: UUID): ResponseEntity<Void> {
        inventoryService.deleteItem(id)
        return ResponseEntity.noContent().build()
    }

    // api.js: getHistory(filters) -> /inventory/history
    @GetMapping("/history")
    fun getHistory(@RequestParam(required = false) limit: Int?): List<com.example.warehouse.model.OperationLog> {
        // Simple implementation returning all sorted by date descending, optionally limited
        // For real filters, we would need Specification or specific repository methods
        return if (limit != null) {
            operationLogRepository.findTop10ByOrderByTimestampDesc() // Assumption for limit, or use Pageable
        } else {
            operationLogRepository.findAllByOrderByTimestampDesc()
        }
    }
}
