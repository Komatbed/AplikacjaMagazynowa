package com.example.warehouse.dto

data class MapUpdateDTO(
    val locationLabel: String,
    val occupancyPercentage: Int,
    val itemsCount: Int,
    val dominantColorHex: String,
    val alertLevel: AlertLevel
)

enum class AlertLevel {
    NONE,
    WARNING,
    CRITICAL
}
