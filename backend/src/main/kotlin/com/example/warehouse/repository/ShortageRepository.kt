package com.example.warehouse.repository

import com.example.warehouse.model.Shortage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ShortageRepository : JpaRepository<Shortage, UUID> {
    fun findByStatus(status: String): List<Shortage>
}
