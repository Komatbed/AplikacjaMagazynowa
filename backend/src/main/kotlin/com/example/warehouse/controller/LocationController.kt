package com.example.warehouse.controller

import com.example.warehouse.config.PalletConfig
import com.example.warehouse.config.WarehouseConfig
import com.example.warehouse.dto.LocationStatusDto
import com.example.warehouse.model.Location
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import jakarta.annotation.PostConstruct
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/locations")
class LocationController(
    private val locationRepository: LocationRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val warehouseConfig: WarehouseConfig,
    private val palletConfig: PalletConfig
) {

    @PostConstruct
    fun initLocations() {
        if (locationRepository.count() == 0L) {
            val pallets = palletConfig.getAllPallets()
            if (pallets.isNotEmpty()) {
                val locations = pallets.map { def ->
                    val details = def.details
                    val zone = details?.zone
                    val row = details?.row ?: 0
                    val paletteNumber = when (zone) {
                        "A" -> 1
                        "B" -> 2
                        "C" -> 3
                        else -> 0
                    }
                    val isWaste = details?.type == "WASTE"

                    Location(
                        rowNumber = row,
                        paletteNumber = paletteNumber,
                        label = def.label,
                        isWastePalette = isWaste
                    )
                }
                locationRepository.saveAll(locations)
            } else {
                val locations = mutableListOf<Location>()
                for (row in 1..25) {
                    locations.add(
                        Location(
                            rowNumber = row,
                            paletteNumber = 1,
                            label = String.format("%02dA", row),
                            isWastePalette = false
                        )
                    )
                    locations.add(
                        Location(
                            rowNumber = row,
                            paletteNumber = 2,
                            label = String.format("%02dB", row),
                            isWastePalette = false
                        )
                    )
                    locations.add(
                        Location(
                            rowNumber = row,
                            paletteNumber = 3,
                            label = String.format("%02dC", row),
                            isWastePalette = true
                        )
                    )
                }
                locationRepository.saveAll(locations)
            }
        }
    }

    @GetMapping("/map")
    fun getWarehouseMap(): List<LocationStatusDto> {
        val locations = locationRepository.findAll()
        val items = inventoryItemRepository.findAll()

        val itemsByLocation = items.groupBy { it.location.id }

        return locations.map { loc ->
            val locItems = itemsByLocation[loc.id] ?: emptyList()
            val profiles = locItems.map { it.profileCode }.distinct().take(3)
            val colors = locItems.mapNotNull { it.coreColor }.filter { it.isNotEmpty() }.distinct().take(3)

            val totalItems = locItems.sumOf { it.quantity }

            val palletDefinition = palletConfig.getPallet(loc.label)
            val configuredCapacity = palletDefinition?.capacity

            val effectiveCapacity = when {
                configuredCapacity != null && configuredCapacity > 0 -> configuredCapacity
                loc.capacity > 0 -> loc.capacity
                else -> warehouseConfig.defaultPalletCapacity
            }

            val occupancyPercent = if (effectiveCapacity > 0) {
                (totalItems * 100) / effectiveCapacity
            } else {
                0
            }

            val overflowThreshold = palletDefinition?.overflowThresholdPercent ?: 100

            LocationStatusDto(
                id = loc.id,
                label = loc.label,
                rowNumber = loc.rowNumber,
                paletteNumber = loc.paletteNumber,
                isWaste = loc.isWastePalette,
                itemCount = totalItems,
                capacity = effectiveCapacity,
                occupancyPercent = occupancyPercent,
                overflowThresholdPercent = overflowThreshold,
                profileCodes = profiles,
                coreColors = colors
            )
        }.sortedBy { it.label }
    }

    @PutMapping("/{id}/capacity")
    fun updateCapacity(@PathVariable id: Int, @RequestBody capacity: Int): Location {
        val location = locationRepository.findById(id).orElseThrow { RuntimeException("Location not found") }
        val updated = location.copy(capacity = capacity)
        return locationRepository.save(updated)
    }
}
