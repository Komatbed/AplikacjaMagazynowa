package com.example.warehouse.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "messages")
data class Message(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(name = "recipient_id", nullable = false)
    val recipientId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "is_read")
    val isRead: Boolean = false,

    @Column(name = "sent_at")
    val sentAt: LocalDateTime = LocalDateTime.now()
)
