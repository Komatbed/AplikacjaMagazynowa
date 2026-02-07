package com.example.warehouse.repository

import com.example.warehouse.model.Message
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {
    fun findBySenderIdOrRecipientId(senderId: UUID, recipientId: UUID): List<Message>
    fun findByRecipientIdAndIsReadFalse(recipientId: UUID): List<Message>
}
