package com.example.warehouse.data.model

data class OptimizationRequest(
    val profileCode: String,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String?,
    val requiredPieces: List<Int>,
    val preferWaste: Boolean = true,
    val reserveWasteLengths: List<Int> = emptyList()
)

data class WasteRecommendationResponse(
    val recommended_item_id: String?,
    val waste_length_mm: Int,
    val cutoff_waste_mm: Int,
    val score: Double,
    val message: String
)

data class CutPlanResponse(
    val totalStockUsed: Int,
    val steps: List<CutStepDto>,
    val totalWasteMm: Int,
    val efficiency: Double
)

data class CutStepDto(
    val sourceItemId: String?,
    val sourceLengthMm: Int,
    val cuts: List<Int>,
    val remainingWasteMm: Int,
    val isNewBar: Boolean,
    val locationLabel: String?
)
