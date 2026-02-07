package com.example.warehouse.controller

import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.ProfileDefinition
import com.example.warehouse.repository.ColorDefinitionRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import org.springframework.web.bind.annotation.*

import com.example.warehouse.service.CoreColorService

@RestController
@RequestMapping("/api/v1/config")
class ConfigurationController(
    private val profileRepository: ProfileDefinitionRepository,
    private val colorRepository: ColorDefinitionRepository,
    private val coreColorService: CoreColorService
) {

    // Core Color Rules
    @GetMapping("/core-rules")
    fun getCoreRules(): Map<String, String> {
        return coreColorService.getMapping()
    }

    // Profiles
    @GetMapping("/profiles")
    fun getProfiles(): List<ProfileDefinition> {
        return profileRepository.findAll()
    }

    @PostMapping("/profiles")
    fun addProfile(@RequestBody profile: ProfileDefinition): ProfileDefinition {
        return profileRepository.save(profile)
    }

    @PutMapping("/profiles/{id}")
    fun updateProfile(@PathVariable id: java.util.UUID, @RequestBody profile: ProfileDefinition): ProfileDefinition {
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
    fun addColor(@RequestBody color: ColorDefinition): ColorDefinition {
        return colorRepository.save(color)
    }

    @PutMapping("/colors/{id}")
    fun updateColor(@PathVariable id: java.util.UUID, @RequestBody color: ColorDefinition): ColorDefinition {
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
