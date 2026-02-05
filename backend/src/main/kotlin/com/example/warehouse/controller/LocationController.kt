package com.example.warehouse.controller

import com.example.warehouse.dto.LocationStatusDto
import com.example.warehouse.model.Location
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import jakarta.annotation.PostConstruct
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/locations")
class LocationController(
    private val locationRepository: LocationRepository,
    private val inventoryItemRepository: InventoryItemRepository
) {

    @PostConstruct
    fun initLocations() {
        if (locationRepository.count() == 0L) {
            val locations = mutableListOf<Location>()
            for (row in 1..25) {
                // Paleta 1 -> A (Całe sztangi)
                locations.add(Location(
                    rowNumber = row,
                    paletteNumber = 1,
                    label = String.format("%02dA", row),
                    isWastePalette = false
                ))
                // Paleta 2 -> B (Całe sztangi)
                locations.add(Location(
                    rowNumber = row,
                    paletteNumber = 2,
                    label = String.format("%02dB", row),
                    isWastePalette = false
                ))
                // Paleta 3 -> C (Odpady)
                locations.add(Location(
                    rowNumber = row,
                    paletteNumber = 3,
                    label = String.format("%02dC", row),
                    isWastePalette = true
                ))
            }
            locationRepository.saveAll(locations)
        }
    }

    @GetMapping("/map")
    fun getWarehouseMap(): List<LocationStatusDto> {
        val locations = locationRepository.findAll()
        val items = inventoryItemRepository.findAll()
        
        // Group items by location ID
        val itemsByLocation = items.groupBy { it.location.id }

        return locations.map { loc ->
            val locItems = itemsByLocation[loc.id] ?: emptyList()
            val profiles = locItems.map { it.profileCode }.distinct().take(3)
            
            LocationStatusDto(
                id = loc.id,
                label = loc.label,
                rowNumber = loc.rowNumber,
                paletteNumber = loc.paletteNumber,
                isWaste = loc.isWastePalette,
                itemCount = locItems.sumOf { it.quantity },
                profileCodes = profiles
            )
        }.sortedBy { it.label }
    }
}
