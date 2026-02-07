package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "issue_reports")
data class IssueReport(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "order_number")
    val orderNumber: String? = null,

    @Column(name = "delivery_date")
    val deliveryDate: LocalDate? = null,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "part_number")
    val partNumber: String? = null,

    @Column(name = "quantity")
    val quantity: Int? = null,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: IssueStatus = IssueStatus.NEW,

    @Column(name = "decision_note", columnDefinition = "TEXT")
    var decisionNote: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class IssueStatus {
    NEW, IN_PROGRESS, RESOLVED, CLOSED, REJECTED
}
