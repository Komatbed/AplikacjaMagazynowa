package com.example.warehouse.dto

import java.util.UUID

data class InventoryItemUpdateDto(
    val id: UUID,
    val newLengthMm: Int
)
