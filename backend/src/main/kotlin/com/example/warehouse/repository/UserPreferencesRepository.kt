package com.example.warehouse.repository

import com.example.warehouse.model.UserPreferences
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserPreferencesRepository : JpaRepository<UserPreferences, UUID> {
    fun findByUserId(userId: UUID): Optional<UserPreferences>
}
