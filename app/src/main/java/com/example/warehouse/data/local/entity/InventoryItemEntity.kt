package com.example.warehouse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val locationLabel: String,
    val profileCode: String,
    val internalColor: String = "UNKNOWN",
    val externalColor: String = "UNKNOWN",
    val coreColor: String? = null,
    val lengthMm: Int,
    val quantity: Int,
    val status: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
