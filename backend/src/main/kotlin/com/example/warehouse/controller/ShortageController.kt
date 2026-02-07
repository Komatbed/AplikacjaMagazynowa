package com.example.warehouse.controller

import com.example.warehouse.model.Shortage
import com.example.warehouse.repository.ShortageRepository
import com.example.warehouse.repository.UserRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/shortages")
class ShortageController(
    private val shortageRepository: ShortageRepository,
    private val userRepository: UserRepository
) {

    // api.js: getShortages() -> /shortages
    @GetMapping
    fun getAllShortages(): List<Shortage> {
        return shortageRepository.findAll()
    }

    // api.js: postShortage(data) -> POST /shortages
    @PostMapping
    fun reportShortage(
        @RequestBody request: ShortageRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Shortage {
        val currentUser = userRepository.findByLogin(userDetails.username).orElseThrow { RuntimeException("User not found") }
        
        val shortage = Shortage(
            reportedById = currentUser.id,
            itemName = request.itemName,
            description = request.description,
            priority = request.priority ?: "NORMAL"
        )
        
        return shortageRepository.save(shortage)
    }

    data class ShortageRequest(
        val itemName: String,
        val description: String?,
        val priority: String?
    )
}
