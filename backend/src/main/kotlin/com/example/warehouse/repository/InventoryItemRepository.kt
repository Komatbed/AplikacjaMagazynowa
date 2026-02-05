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

    @org.springframework.data.jpa.repository.Query("SELECT i FROM InventoryItem i WHERE (:location IS NULL OR i.location.label = :location) AND (:profileCode IS NULL OR i.profileCode = :profileCode) AND (:internalColor IS NULL OR i.internalColor = :internalColor) AND (:externalColor IS NULL OR i.externalColor = :externalColor) AND (:coreColor IS NULL OR i.coreColor = :coreColor)")
    fun findFiltered(location: String?, profileCode: String?, internalColor: String?, externalColor: String?, coreColor: String?): List<InventoryItem>
}
