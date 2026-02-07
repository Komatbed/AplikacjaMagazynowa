package com.example.warehouse.controller

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class NotificationMessage(
    val title: String,
    val message: String,
    val type: String = "INFO" // INFO, WARNING, ERROR
)

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @PostMapping("/send")
    fun sendNotification(@RequestBody notification: NotificationMessage) {
        // Broadcast to all subscribers
        messagingTemplate.convertAndSend("/topic/notifications", notification)
    }
}
