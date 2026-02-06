package com.example.warehouse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g. "P1 Winchester"
    val profileCode: String,
    val externalColor: String,
    val internalColor: String,
    val createdAt: Long = System.currentTimeMillis()
)
