package com.example.warehouse.model

data class OptimizationRequest(
    val profileCode: String,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String? = null,
    val requiredPieces: List<Int>, // List of lengths in mm
    val preferWaste: Boolean = true,
    val reserveWasteLengths: List<Int> = emptyList() // e.g. [1200, 850]
)

data class CutPlan(
    val totalStockUsed: Int, // Count of bars/wastes used
    val steps: List<CutStep>,
    val totalWasteMm: Int,
    val efficiency: Double // Percentage
)

data class CutStep(
    val sourceItemId: String?, // Null if new bar
    val sourceLengthMm: Int,
    val cuts: List<Int>, // Lengths to cut
    val remainingWasteMm: Int,
    val isNewBar: Boolean,
    val locationLabel: String? // Where to find the source
)
