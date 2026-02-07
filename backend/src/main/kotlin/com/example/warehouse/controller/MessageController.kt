package com.example.warehouse.controller

import com.example.warehouse.model.Message
import com.example.warehouse.model.User
import com.example.warehouse.repository.MessageRepository
import com.example.warehouse.repository.UserRepository
import com.example.warehouse.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/messages")
class MessageController(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) {

    // Get all users for chat list
    // Corresponds to api.js: getUsers() -> /messages/users (Wait, api.js path is /messages/users, so backend should be /api/v1/messages/users if we map prefix globally)
    // api.js uses base url http://localhost:8080/api/v1. So api.request('/messages/users') -> /api/v1/messages/users. Correct.
    @GetMapping("/users")
    fun getUsers(): List<UserResponse> {
        return userRepository.findAll().map { 
            UserResponse(it.id, it.login, it.fullName, it.role.name) 
        }
    }

    // Get chat history with a user
    // api.js: getMessages(userId) -> /messages/chat/${userId}
    @GetMapping("/chat/{userId}")
    fun getChatHistory(
        @PathVariable userId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): List<Message> {
        // Find current user ID from login
        val currentUser = userRepository.findByLogin(userDetails.username).orElseThrow { RuntimeException("User not found") }
        val currentUserId = currentUser.id
        
        // Simple implementation: get all messages where (sender=me AND recipient=them) OR (sender=them AND recipient=me)
        // Sort by time
        val allMessages = messageRepository.findBySenderIdOrRecipientId(currentUserId, currentUserId)
        
        return allMessages.filter { 
            (it.senderId == currentUserId && it.recipientId == userId) || 
            (it.senderId == userId && it.recipientId == currentUserId)
        }.sortedBy { it.sentAt }
    }

    // Send message
    // api.js: sendMessage(userId, text) -> POST /messages/chat/${userId}
    @PostMapping("/chat/{recipientId}")
    fun sendMessage(
        @PathVariable recipientId: UUID,
        @RequestBody request: MessageRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Message {
        val currentUser = userRepository.findByLogin(userDetails.username).orElseThrow { RuntimeException("User not found") }
        
        val message = Message(
            senderId = currentUser.id,
            recipientId = recipientId,
            content = request.text
        )
        
        return messageRepository.save(message)
    }

    data class UserResponse(val id: UUID, val login: String, val fullName: String, val role: String)
    data class MessageRequest(val text: String)
}
