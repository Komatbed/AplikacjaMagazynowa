package com.example.warehouse.dto

data class InventoryWasteRequest(
    val sourceProfileId: String?, // Optional
    val lengthMm: Int,
    val locationLabel: String,
    val quantity: Int,
    val profileCode: String,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String?
)
