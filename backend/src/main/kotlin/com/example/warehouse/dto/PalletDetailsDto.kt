package com.example.warehouse.dto

data class PalletDetailsDto(
    val label: String,
    val zone: String?,
    val row: Int?,
    val type: String?,
    val capacity: Int,
    val occupancyPercentage: Int,
    val totalItems: Int,
    val itemsAvailable: Int,
    val itemsReserved: Int,
    val itemsWaste: Int,
    val profiles: List<String>,
    val coreColors: List<String>
)

data class PalletSuggestionRequest(
    val profileCode: String,
    val lengthMm: Int,
    val quantity: Int = 1,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String? = null,
    val isWaste: Boolean = false
)

data class PalletSuggestionResponse(
    val label: String?
)
