package com.example.warehouse.controller

import com.example.warehouse.dto.InventoryTakeRequest
import com.example.warehouse.dto.InventoryTakeResponse
import com.example.warehouse.dto.InventoryWasteRequest
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.service.InventoryService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryService: InventoryService,
    private val profileDefinitionRepository: com.example.warehouse.repository.ProfileDefinitionRepository,
    private val colorDefinitionRepository: com.example.warehouse.repository.ColorDefinitionRepository
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
        @RequestParam(required = false) coreColor: String?
    ): List<InventoryItem> {
        return inventoryItemRepository.findFiltered(location, profileCode, internalColor, externalColor, coreColor)
    }

    @PostMapping("/take")
    fun takeItem(@RequestBody request: InventoryTakeRequest): InventoryTakeResponse {
        return inventoryService.takeItem(request)
    }

    @PostMapping("/waste")
    fun registerWaste(@RequestBody request: InventoryWasteRequest): InventoryItem {
        return inventoryService.registerWaste(request)
    }

    @PutMapping("/items/{id}/length")
    fun updateItemLength(@PathVariable id: java.util.UUID, @RequestBody length: Int): InventoryItem {
        return inventoryService.updateItemLength(id, length)
    }
}
