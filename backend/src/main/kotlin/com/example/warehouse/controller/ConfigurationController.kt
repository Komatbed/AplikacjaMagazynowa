package com.example.warehouse.controller

import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.ProfileDefinition
import com.example.warehouse.repository.ColorDefinitionRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/config")
class ConfigurationController(
    private val profileRepository: ProfileDefinitionRepository,
    private val colorRepository: ColorDefinitionRepository
) {

    // Profiles
    @GetMapping("/profiles")
    fun getProfiles(): List<ProfileDefinition> {
        return profileRepository.findAll()
    }

    @PostMapping("/profiles")
    fun addProfile(@RequestBody profile: ProfileDefinition): ProfileDefinition {
        return profileRepository.save(profile)
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

    @DeleteMapping("/colors/{id}")
    fun deleteColor(@PathVariable id: java.util.UUID) {
        colorRepository.deleteById(id)
    }
}
