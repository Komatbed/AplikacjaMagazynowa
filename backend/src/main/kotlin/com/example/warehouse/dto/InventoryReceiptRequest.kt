package com.example.warehouse.dto

data class InventoryReceiptRequest(
    val locationLabel: String,
    val profileCode: String,
    val lengthMm: Int,
    val quantity: Int,
    val internalColor: String,
    val externalColor: String,
    val coreColor: String? = null
)
