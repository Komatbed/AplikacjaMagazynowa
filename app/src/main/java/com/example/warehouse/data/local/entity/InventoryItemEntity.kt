package com.example.warehouse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["profileCode"]),
        Index(value = ["internalColor"]),
        Index(value = ["locationLabel"])
    ]
)
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
