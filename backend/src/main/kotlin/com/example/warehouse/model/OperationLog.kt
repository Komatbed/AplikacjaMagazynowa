package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "operation_logs")
data class OperationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "operation_type", nullable = false)
    val operationType: String,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "device_id")
    val deviceId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    val inventoryItem: InventoryItem? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    val location: Location? = null,

    @Column(name = "quantity_change", nullable = false)
    val quantityChange: Int,

    @Column(name = "reason")
    val reason: String? = null,

    @Column(name = "photo_url")
    val photoUrl: String? = null,

    @Column(name = "timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(name = "synced_at")
    val syncedAt: LocalDateTime? = null
)
