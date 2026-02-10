package com.example.warehouse.dto

import java.util.UUID

data class ReservationRequest(
    val itemId: UUID,
    val quantity: Int,
    val reservedBy: String,
    val notes: String? = null
)

data class ReservationCancelRequest(
    val itemId: UUID
)
