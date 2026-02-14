package com.example.warehouse.data.model

data class InventoryItemUpdatePayload(
    val id: String,
    val length: Int
)

data class InventoryItemDeletePayload(
    val id: String
)

data class ChangePasswordWithOldRequest(
    val oldPassword: String,
    val newPassword: String
)
