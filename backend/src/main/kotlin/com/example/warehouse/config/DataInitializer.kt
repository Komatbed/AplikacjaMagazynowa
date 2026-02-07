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

@Configuration
class DataInitializer {

    @Bean
    fun initData(
        profileRepo: ProfileDefinitionRepository,
        colorRepo: ColorDefinitionRepository,
        locationRepo: LocationRepository,
        inventoryRepo: InventoryItemRepository,
        authService: AuthService
    ): CommandLineRunner {
        return CommandLineRunner {
            // Seed Admin User
            authService.createDefaultAdmin()
            println("Seeded default admin user")

            // Seed Profiles
            if (profileRepo.count() == 0L) {
                val profiles = listOf(
                    ProfileDefinition(code = "ALU-100", description = "Aluminium 100mm"),
                    ProfileDefinition(code = "ALU-200", description = "Aluminium 200mm"),
                    ProfileDefinition(code = "PVC-WINDOW", description = "PVC Window Profile"),
                    ProfileDefinition(code = "STEEL-BOX", description = "Steel Box Section")
                )
                profileRepo.saveAll(profiles)
                println("Seeded initial profiles")
            }

            // Seed Colors
            if (colorRepo.count() == 0L) {
                val colors = listOf(
                    ColorDefinition(code = "RAL9016", description = "Traffic White"),
                    ColorDefinition(code = "RAL7016", description = "Anthracite Grey"),
                    ColorDefinition(code = "RAL9005", description = "Jet Black"),
                    ColorDefinition(code = "RAW", description = "Unpainted/Raw")
                )
                colorRepo.saveAll(colors)
                println("Seeded initial colors")
            }

            // Seed Locations
            if (locationRepo.count() == 0L) {
                val locations = mutableListOf<Location>()
                // Create A-01-01 to A-05-02
                for (row in 1..5) {
                    for (palette in 1..2) {
                        val label = "A-%02d-%02d".format(row, palette)
                        locations.add(Location(rowNumber = row, paletteNumber = palette, label = label, isWastePalette = false))
                    }
                }
                // Add one waste palette
                locations.add(Location(rowNumber = 99, paletteNumber = 1, label = "WASTE-01", isWastePalette = true))
                
                locationRepo.saveAll(locations)
                println("Seeded initial locations")
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
}
