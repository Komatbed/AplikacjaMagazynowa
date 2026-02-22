package com.example.warehouse.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class InventoryTakeRequest(
    @field:NotBlank(message = "Etykieta lokalizacji jest wymagana")
    val locationLabel: String,
    val profileCode: String?,
    @field:Min(value = 1, message = "Długość musi być dodatnia")
    val lengthMm: Int,
    @field:Min(value = 1, message = "Ilość musi być dodatnia")
    val quantity: Int,
    val reason: String,
    val force: Boolean = false
)

data class InventoryTakeResponse(
    val status: String,
    val newQuantity: Int,
    val warning: String? = null,
    val code: String? = null
)
