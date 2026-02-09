package com.example.warehouse.repository

import com.example.warehouse.model.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : JpaRepository<Location, Int> {
    fun findByLabel(label: String): Location?
}
