package com.example.warehouse.config

import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.InventoryItem
import com.example.warehouse.model.ItemStatus
import com.example.warehouse.model.Location
import com.example.warehouse.model.ProfileDefinition
import com.example.warehouse.repository.ColorDefinitionRepository
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.LocationRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import com.example.warehouse.service.AuthService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Random

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ClassPathResource
import java.io.IOException

@Configuration
class DataInitializer(
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun initData(
        profileRepo: ProfileDefinitionRepository,
        colorRepo: ColorDefinitionRepository,
        locationRepo: LocationRepository,
        inventoryRepo: InventoryItemRepository,
        authService: AuthService,
        warehouseConfig: WarehouseConfig,
        palletConfig: PalletConfig
    ): CommandLineRunner {
        return CommandLineRunner {
            // Seed Admin User
            authService.createDefaultAdmin()
            println("Seeded default admin user")

            // Seed Profiles
            if (profileRepo.count() == 0L) {
                val loadedProfiles = loadProfilesFromJson()
                val profiles = loadedProfiles ?: listOf(
                    ProfileDefinition(code = "ALU-100", description = "Aluminium 100mm"),
                    ProfileDefinition(code = "ALU-200", description = "Aluminium 200mm"),
                    ProfileDefinition(code = "PVC-WINDOW", description = "PVC Window Profile"),
                    ProfileDefinition(code = "STEEL-BOX", description = "Steel Box Section")
                )
                profileRepo.saveAll(profiles)
                println("Seeded initial profiles (${if (loadedProfiles != null) "from JSON" else "defaults"})")
            }

            // Seed Colors
            if (colorRepo.count() == 0L) {
                val loadedColors = loadColorsFromJson()
                val colors = loadedColors ?: listOf(
                    ColorDefinition(code = "RAL9016", description = "Traffic White"),
                    ColorDefinition(code = "RAL7016", description = "Anthracite Grey"),
                    ColorDefinition(code = "RAL9005", description = "Jet Black"),
                    ColorDefinition(code = "RAW", description = "Unpainted/Raw")
                )
                colorRepo.saveAll(colors)
                println("Seeded initial colors (${if (loadedColors != null) "from JSON" else "defaults"})")
            }

            if (locationRepo.count() == 0L) {
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
                        val capacity = def.capacity ?: warehouseConfig.defaultPalletCapacity

                        Location(
                            rowNumber = row,
                            paletteNumber = paletteNumber,
                            label = def.label,
                            isWastePalette = isWaste,
                            capacity = capacity
                        )
                    }
                    locationRepo.saveAll(locations)
                    println("Seeded initial locations from pallet_config (${locations.size})")
                } else {
                    val locations = mutableListOf<Location>()
                    val capacity = warehouseConfig.defaultPalletCapacity
                    for (row in 1..5) {
                        for (palette in 1..2) {
                            val label = "A-%02d-%02d".format(row, palette)
                            locations.add(
                                Location(
                                    rowNumber = row,
                                    paletteNumber = palette,
                                    label = label,
                                    isWastePalette = false,
                                    capacity = capacity
                                )
                            )
                        }
                    }
                    locations.add(
                        Location(
                            rowNumber = 99,
                            paletteNumber = 1,
                            label = "WASTE-01",
                            isWastePalette = true,
                            capacity = capacity
                        )
                    )
                    locationRepo.saveAll(locations)
                    println("Seeded initial locations (fallback, capacity: $capacity)")
                }
            }

            // Seed Inventory Items
            if (inventoryRepo.count() == 0L) {
                val locations = locationRepo.findAll()
                val profiles = profileRepo.findAll()
                val colors = colorRepo.findAll()
                val random = Random()
                
                if (locations.isNotEmpty() && profiles.isNotEmpty() && colors.isNotEmpty()) {
                    val items = mutableListOf<InventoryItem>()
                    
                    // Create some random items
                    for (i in 1..20) {
                        val loc = locations[random.nextInt(locations.size)]
                        val prof = profiles[random.nextInt(profiles.size)]
                        val col1 = colors[random.nextInt(colors.size)]
                        val col2 = colors[random.nextInt(colors.size)]
                        
                        items.add(InventoryItem(
                            location = loc,
                            profileCode = prof.code,
                            internalColor = col1.code,
                            externalColor = col2.code,
                            lengthMm = 1000 + random.nextInt(2000), // 1000-3000mm
                            quantity = 1 + random.nextInt(10),
                            status = ItemStatus.AVAILABLE
                        ))
                    }
                    
                    inventoryRepo.saveAll(items)
                    println("Seeded initial inventory items")
                }
            }
        }
    }

    private fun loadProfilesFromJson(): List<ProfileDefinition>? {
        return try {
            val resource = ClassPathResource("initial_data/profiles.json")
            if (resource.exists()) {
                objectMapper.readValue(resource.inputStream, object : TypeReference<List<ProfileDefinition>>() {})
            } else null
        } catch (e: Exception) {
            println("Failed to load profiles.json: ${e.message}")
            null
        }
    }

    private fun loadColorsFromJson(): List<ColorDefinition>? {
        return try {
            val resource = ClassPathResource("initial_data/colors.json")
            if (resource.exists()) {
                objectMapper.readValue(resource.inputStream, object : TypeReference<List<ColorDefinition>>() {})
            } else null
        } catch (e: Exception) {
            println("Failed to load colors.json: ${e.message}")
            null
        }
    }
}
