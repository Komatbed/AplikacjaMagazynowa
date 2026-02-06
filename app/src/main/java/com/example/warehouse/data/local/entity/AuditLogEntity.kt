package com.example.warehouse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "ADD", "TAKE", "WASTE", "UPDATE_LENGTH", "CONFIG_CHANGE"
    val itemType: String, // "INVENTORY", "PROFILE", "COLOR"
    val details: String // Description
)
