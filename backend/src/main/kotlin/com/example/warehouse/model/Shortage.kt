package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "shortages")
data class Shortage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "reported_by_id")
    val reportedById: UUID?,

    @Column(name = "item_name", nullable = false)
    val itemName: String,

    @Column(columnDefinition = "TEXT")
    val description: String?,

    @Column(length = 20)
    val priority: String = "NORMAL",

    @Column(length = 20)
    val status: String = "NEW",

    @Column(name = "reported_at")
    val reportedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "resolved_at")
    val resolvedAt: LocalDateTime? = null
)
