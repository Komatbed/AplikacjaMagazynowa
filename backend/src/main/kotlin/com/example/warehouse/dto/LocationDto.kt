package com.example.warehouse.dto

data class LocationStatusDto(
    val id: Int,
    val label: String,
    val rowNumber: Int,
    val paletteNumber: Int,
    val isWaste: Boolean,
    val itemCount: Int,
    val capacity: Int,
    val occupancyPercent: Int,
    val overflowThresholdPercent: Int,
    val profileCodes: List<String>,
    val coreColors: List<String>
)
