package com.example.warehouse.dto

data class InventoryTakeRequest(
    val locationLabel: String,
    val profileCode: String?,
    val lengthMm: Int,
    val quantity: Int,
    val reason: String, // PRODUCTION, DAMAGE, SAMPLE
    val force: Boolean = false
)

data class InventoryTakeResponse(
    val status: String, // SUCCESS, WARNING
    val newQuantity: Int,
    val warning: String? = null,
    val code: String? = null
)
