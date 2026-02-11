package com.example.warehouse.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class InventoryReceiptRequest(
    @field:NotBlank(message = "Etykieta lokalizacji jest wymagana")
    val locationLabel: String,

    @field:NotBlank(message = "Kod profilu jest wymagany")
    val profileCode: String,

    @field:Min(value = 1, message = "Długość musi być większa od 0")
    val lengthMm: Int,

    @field:Min(value = 1, message = "Ilość musi być większa od 0")
    val quantity: Int,

    @field:NotBlank(message = "Kolor wewnętrzny jest wymagany")
    val internalColor: String,

    @field:NotBlank(message = "Kolor zewnętrzny jest wymagany")
    val externalColor: String,

    val coreColor: String? = null
)
