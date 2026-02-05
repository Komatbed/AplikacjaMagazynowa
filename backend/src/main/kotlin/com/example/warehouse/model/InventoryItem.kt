package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_items")
data class InventoryItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    val location: Location,

    @Column(name = "profile_code", nullable = false)
    val profileCode: String,

    @Column(name = "internal_color", nullable = false)
    val internalColor: String = "UNKNOWN",

    @Column(name = "external_color", nullable = false)
    val externalColor: String = "UNKNOWN",

    @Column(name = "core_color")
    val coreColor: String? = null,

    @Column(name = "length_mm", nullable = false)
    var lengthMm: Int,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: ItemStatus = ItemStatus.AVAILABLE,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ItemStatus {
    AVAILABLE, RESERVED, DAMAGED
}
