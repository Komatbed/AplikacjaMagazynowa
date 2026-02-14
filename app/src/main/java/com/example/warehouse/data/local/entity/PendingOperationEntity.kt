package com.example.warehouse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: OperationType,
    val payloadJson: String, // Serialized Request (InventoryWasteRequest or InventoryTakeRequest)
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

enum class OperationType {
    REGISTER_WASTE,
    TAKE_ITEM,
    UPDATE_ITEM_LENGTH,
    ADD_PROFILE,
    ADD_COLOR,
    ADD_ITEM,
    DELETE_ITEM
}
