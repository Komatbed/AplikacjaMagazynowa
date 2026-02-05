package com.example.warehouse.repository

import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.ProfileDefinition
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileDefinitionRepository : JpaRepository<ProfileDefinition, UUID> {
    fun findByCode(code: String): ProfileDefinition?
}

interface ColorDefinitionRepository : JpaRepository<ColorDefinition, UUID> {
    fun findByCode(code: String): ColorDefinition?
}
