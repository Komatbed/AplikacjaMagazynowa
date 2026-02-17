package com.example.warehouse.controller

import com.example.warehouse.config.WarehouseConfig
import com.example.warehouse.config.MuntinsV3Config
import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.ProfileDefinition
import com.example.warehouse.repository.ColorDefinitionRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

import com.example.warehouse.service.CoreColorService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import java.io.File

@RestController
@RequestMapping("/api/v1/config")
class ConfigurationController(
    private val profileRepository: ProfileDefinitionRepository,
    private val colorRepository: ColorDefinitionRepository,
    private val coreColorService: CoreColorService,
    private val warehouseConfig: WarehouseConfig,
    private val muntinsV3Config: MuntinsV3Config,
    private val objectMapper: ObjectMapper
) {

    // Warehouse Config
    @GetMapping("/warehouse")
    fun getWarehouseConfig(): Map<String, Any> {
        return mapOf(
            "lowStockThreshold" to warehouseConfig.lowStockThreshold,
            "defaultPalletCapacity" to warehouseConfig.defaultPalletCapacity,
            "reserveWasteLengths" to warehouseConfig.reserveWasteLengths,
            "customMultiCoreColors" to warehouseConfig.customMultiCoreColors,
            "ral9001EligibleColors" to warehouseConfig.ral9001EligibleColors
        )
    }

    // Reload Defaults
    @PostMapping("/reload-defaults")
    fun reloadDefaults(): Map<String, String> {
        val stats = mutableMapOf<String, String>()
        
        // Reload Beans
        warehouseConfig.reload()
        coreColorService.reload()
        muntinsV3Config.reload()
        stats["beans"] = "Reloaded WarehouseConfig, CoreColorService and MuntinsV3Config"
        
        // Reload Profiles
        try {
            val profiles = loadProfiles()
            if (profiles != null) {
                var added = 0
                var updated = 0
                profiles.forEach { profile ->
                    val existing = profileRepository.findByCode(profile.code)
                    if (existing != null) {
                        profileRepository.save(existing.copy(
                            description = profile.description,
                            standardLengthMm = profile.standardLengthMm,
                            heightMm = profile.heightMm,
                            widthMm = profile.widthMm,
                            beadHeightMm = profile.beadHeightMm,
                            beadAngle = profile.beadAngle,
                            system = profile.system,
                            manufacturer = profile.manufacturer,
                            type = profile.type
                        ))
                        updated++
                    } else {
                        profileRepository.save(profile)
                        added++
                    }
                }
                stats["profiles"] = "Added: $added, Updated: $updated"
            } else {
                stats["profiles"] = "Failed to load profiles.json"
            }
        } catch (e: Exception) {
            stats["profiles"] = "Error: ${e.message}"
        }

        // Reload Colors
        try {
            val colors = loadColors()
            if (colors != null) {
                var added = 0
                var updated = 0
                colors.forEach { color ->
                    val existing = colorRepository.findByCode(color.code)
                    if (existing != null) {
                        colorRepository.save(existing.copy(
                            description = color.description,
                            name = color.name,
                            paletteCode = color.paletteCode,
                            vekaCode = color.vekaCode,
                            type = color.type,
                            foilManufacturer = color.foilManufacturer
                        ))
                        updated++
                    } else {
                        colorRepository.save(color)
                        added++
                    }
                }
                stats["colors"] = "Added: $added, Updated: $updated"
            } else {
                stats["colors"] = "Failed to load colors.json"
            }
        } catch (e: Exception) {
            stats["colors"] = "Error: ${e.message}"
        }

        return stats
    }

    private fun loadProfiles(): List<ProfileDefinition>? {
        // Try FileSystem first (for dev/hot reload)
        val file = File("src/main/resources/initial_data/profiles.json")
        if (file.exists()) {
             return objectMapper.readValue(file, object : TypeReference<List<ProfileDefinition>>() {})
        }
        // Fallback to Classpath
        val resource = ClassPathResource("initial_data/profiles.json")
        if (resource.exists()) {
            return objectMapper.readValue(resource.inputStream, object : TypeReference<List<ProfileDefinition>>() {})
        }
        return null
    }

    private fun loadColors(): List<ColorDefinition>? {
        // Try FileSystem first
        val file = File("src/main/resources/initial_data/colors.json")
        if (file.exists()) {
             return objectMapper.readValue(file, object : TypeReference<List<ColorDefinition>>() {})
        }
        // Fallback to Classpath
        val resource = ClassPathResource("initial_data/colors.json")
        if (resource.exists()) {
            return objectMapper.readValue(resource.inputStream, object : TypeReference<List<ColorDefinition>>() {})
        }
        return null
    }

    // Core Color Rules
    @GetMapping("/core-rules")
    fun getCoreRules(): Map<String, String> {
        val mapping = coreColorService.getMapping()
        if (mapping.isEmpty()) return emptyMap()

        val normalized = mapping.mapKeys { it.key.lowercase() }
        val result = mutableMapOf<String, String>()

        colorRepository.findAll().forEach { color ->
            val core = normalized[color.name.lowercase()]
            if (core != null) {
                result[color.code] = core
            }
        }

        return result
    }

    @GetMapping("/core-map")
    fun getCoreMap(): Map<String, String> {
        val file = File("src/main/resources/core_color_map.json")
        if (file.exists()) {
            return objectMapper.readValue(
                file,
                object : TypeReference<Map<String, String>>() {}
            )
        }
        val resource = ClassPathResource("core_color_map.json")
        if (resource.exists()) {
            return objectMapper.readValue(
                resource.inputStream,
                object : TypeReference<Map<String, String>>() {}
            )
        }
        return emptyMap()
    }

    @PutMapping("/core-map")
    fun updateCoreMap(@RequestBody map: Map<String, String>): Map<String, String> {
        val file = File("src/main/resources/core_color_map.json")
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, map)
        coreColorService.reload()
        return map
    }

    @GetMapping("/muntins-v3")
    fun getMuntinsV3(): Map<String, Any> {
        return mapOf(
            "profiles" to muntinsV3Config.profiles,
            "beads" to muntinsV3Config.beads,
            "muntins" to muntinsV3Config.muntins
        )
    }

    // Profiles
    @GetMapping("/profiles")
    fun getProfiles(): List<ProfileDefinition> {
        return profileRepository.findAll()
    }

    @PostMapping("/profiles")
    fun addProfile(@Valid @RequestBody profile: ProfileDefinition): ProfileDefinition {
        return profileRepository.save(profile)
    }

    @PutMapping("/profiles/{id}")
    fun updateProfile(@PathVariable id: java.util.UUID, @Valid @RequestBody profile: ProfileDefinition): ProfileDefinition {
        // Ensure ID matches or simple save if we assume it overwrites
        // Ideally fetch and update
        val existing = profileRepository.findById(id).orElseThrow { RuntimeException("Profile not found") }
        val updated = existing.copy(
            code = profile.code,
            description = profile.description,
            standardLengthMm = profile.standardLengthMm,
            heightMm = profile.heightMm,
            widthMm = profile.widthMm,
            system = profile.system,
            manufacturer = profile.manufacturer
        )
        return profileRepository.save(updated)
    }

    @DeleteMapping("/profiles/{id}")
    fun deleteProfile(@PathVariable id: java.util.UUID) {
        profileRepository.deleteById(id)
    }

    // Colors
    @GetMapping("/colors")
    fun getColors(): List<ColorDefinition> {
        return colorRepository.findAll()
    }

    @PostMapping("/colors")
    fun addColor(@Valid @RequestBody color: ColorDefinition): ColorDefinition {
        return colorRepository.save(color)
    }

    @PutMapping("/colors/{id}")
    fun updateColor(@PathVariable id: java.util.UUID, @Valid @RequestBody color: ColorDefinition): ColorDefinition {
        val existing = colorRepository.findById(id).orElseThrow { RuntimeException("Color not found") }
        val updated = existing.copy(
            code = color.code,
            description = color.description,
            name = color.name,
            paletteCode = color.paletteCode,
            vekaCode = color.vekaCode,
            type = color.type,
            foilManufacturer = color.foilManufacturer
        )
        return colorRepository.save(updated)
    }

    @DeleteMapping("/colors/{id}")
    fun deleteColor(@PathVariable id: java.util.UUID) {
        colorRepository.deleteById(id)
    }
}
