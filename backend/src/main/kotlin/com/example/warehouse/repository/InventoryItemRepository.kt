package com.example.warehouse.repository

import com.example.warehouse.model.InventoryItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InventoryItemRepository : JpaRepository<InventoryItem, UUID> {
    fun findByLocation_Label(label: String): List<InventoryItem>
    fun findByProfileCode(profileCode: String): List<InventoryItem>
    
    @org.springframework.data.jpa.repository.Query("SELECT i FROM InventoryItem i WHERE i.profileCode = :profileCode AND i.internalColor = :internalColor AND i.externalColor = :externalColor AND (:coreColor IS NULL OR i.coreColor = :coreColor)")
    fun findMatchingItems(profileCode: String, internalColor: String, externalColor: String, coreColor: String?): List<InventoryItem>

    @org.springframework.data.jpa.repository.Query("SELECT i FROM InventoryItem i WHERE (:location IS NULL OR LOWER(i.location.label) LIKE LOWER(CONCAT('%', :location, '%'))) AND (:profileCode IS NULL OR LOWER(i.profileCode) LIKE LOWER(CONCAT('%', :profileCode, '%'))) AND (:internalColor IS NULL OR LOWER(i.internalColor) LIKE LOWER(CONCAT('%', :internalColor, '%'))) AND (:externalColor IS NULL OR LOWER(i.externalColor) LIKE LOWER(CONCAT('%', :externalColor, '%'))) AND (:coreColor IS NULL OR LOWER(i.coreColor) LIKE LOWER(CONCAT('%', :coreColor, '%')))")
    fun findFiltered(location: String?, profileCode: String?, internalColor: String?, externalColor: String?, coreColor: String?, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<InventoryItem>
}
