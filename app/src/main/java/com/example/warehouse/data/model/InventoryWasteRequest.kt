package com.example.warehouse.data.model

data class InventoryWasteRequest(
    val profileCode: String,
    val lengthMm: Int,
    val quantity: Int,
    val locationLabel: String,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String? = null,
    val reason: String = "PRODUCTION_WASTE"
)
