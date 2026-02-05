package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "issue_reports")
data class IssueReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "description", nullable = false, length = 1000)
    val description: String,

    @Column(name = "profile_code")
    val profileCode: String? = null,

    @Column(name = "location_label")
    val locationLabel: String? = null,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: IssueStatus = IssueStatus.NEW,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class IssueStatus {
    NEW, IN_PROGRESS, RESOLVED, CLOSED
}
