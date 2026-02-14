package com.example.warehouse.controller

import com.example.warehouse.model.Role
import com.example.warehouse.model.User
import com.example.warehouse.repository.UserRepository
import com.example.warehouse.repository.UserPreferencesRepository
import com.example.warehouse.model.UserPreferences
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UsersController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val preferencesRepository: UserPreferencesRepository
)
{
    @GetMapping
    fun getUsers(): List<UserResponse> {
        return userRepository.findAll().map { UserResponse.from(it) }
    }
    
    @PostMapping
    fun createUser(@RequestBody req: CreateUserRequest): ResponseEntity<UserResponse> {
        if (userRepository.existsByLogin(req.username)) {
            return ResponseEntity.badRequest().build()
        }
        val role = Role.valueOf(req.role.uppercase())
        val user = User(
            login = req.username,
            passwordHash = passwordEncoder.encode(req.password),
            fullName = req.fullName ?: req.username,
            role = role,
            mustChangePassword = false
        )
        val saved = userRepository.save(user)
        return ResponseEntity.ok(UserResponse.from(saved))
    }
    
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        return if (userRepository.existsById(id)) {
            userRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PutMapping("/{id}/role")
    fun changeRole(@PathVariable id: UUID, @RequestBody req: RoleChangeRequest): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        user.role = Role.valueOf(req.role.uppercase())
        val saved = userRepository.save(user)
        return ResponseEntity.ok(UserResponse.from(saved))
    }
    
    @PostMapping("/{id}/reset-password")
    fun resetPassword(@PathVariable id: UUID, @RequestBody req: PasswordResetRequest): ResponseEntity<Void> {
        val user = userRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        user.passwordHash = passwordEncoder.encode(req.newPassword)
        user.mustChangePassword = true
        userRepository.save(user)
        return ResponseEntity.noContent().build()
    }
    
    @PutMapping("/me/password")
    fun changeOwnPassword(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody req: PasswordResetRequest
    ): ResponseEntity<Void> {
        val current = userRepository.findByLogin(principal.username).orElse(null) ?: return ResponseEntity.notFound().build()
        current.passwordHash = passwordEncoder.encode(req.newPassword)
        current.mustChangePassword = false
        userRepository.save(current)
        return ResponseEntity.noContent().build()
    }
    
    @PutMapping("/me/password-with-old")
    fun changeOwnPasswordWithOld(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody req: ChangePasswordWithOldRequest
    ): ResponseEntity<Void> {
        val current = userRepository.findByLogin(principal.username).orElse(null) ?: return ResponseEntity.notFound().build()
        if (!passwordEncoder.matches(req.oldPassword, current.passwordHash)) {
            return ResponseEntity.status(403).build()
        }
        current.passwordHash = passwordEncoder.encode(req.newPassword)
        current.mustChangePassword = false
        userRepository.save(current)
        return ResponseEntity.noContent().build()
    }
    
    @PutMapping("/{id}/require-password-change")
    fun requirePasswordChange(@PathVariable id: UUID): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        user.mustChangePassword = true
        val saved = userRepository.save(user)
        return ResponseEntity.ok(UserResponse.from(saved))
    }
    
    @GetMapping("/me/preferences")
    fun getOwnPreferences(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<UserPreferencesDto> {
        val me = userRepository.findByLogin(principal.username).orElse(null) ?: return ResponseEntity.notFound().build()
        val existing = preferencesRepository.findByUserId(me.id).orElse(null)
        val prefs = existing ?: preferencesRepository.save(UserPreferences(userId = me.id))
        return ResponseEntity.ok(UserPreferencesDto.from(prefs))
    }
    
    @PutMapping("/me/preferences")
    fun updateOwnPreferences(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody req: UserPreferencesUpdateRequest
    ): ResponseEntity<UserPreferencesDto> {
        val me = userRepository.findByLogin(principal.username).orElse(null) ?: return ResponseEntity.notFound().build()
        val prefs = preferencesRepository.findByUserId(me.id).orElse(UserPreferences(userId = me.id))
        req.favoriteProfileCodes?.let { prefs.favoriteProfileCodes = it }
        req.favoriteColorCodes?.let { prefs.favoriteColorCodes = it }
        req.preferredProfileOrder?.let { prefs.preferredProfileOrder = it }
        req.preferredColorOrder?.let { prefs.preferredColorOrder = it }
        val saved = preferencesRepository.save(prefs)
        return ResponseEntity.ok(UserPreferencesDto.from(saved))
    }
    
    data class CreateUserRequest(val username: String, val password: String, val fullName: String?, val role: String)
    data class RoleChangeRequest(val role: String)
    data class PasswordResetRequest(val newPassword: String)
    data class ChangePasswordWithOldRequest(val oldPassword: String, val newPassword: String)
    data class UserPreferencesDto(val favoriteProfileCodes: String, val favoriteColorCodes: String, val preferredProfileOrder: String, val preferredColorOrder: String) {
        companion object {
            fun from(p: UserPreferences) = UserPreferencesDto(p.favoriteProfileCodes, p.favoriteColorCodes, p.preferredProfileOrder, p.preferredColorOrder)
        }
    }
    data class UserPreferencesUpdateRequest(
        val favoriteProfileCodes: String?,
        val favoriteColorCodes: String?,
        val preferredProfileOrder: String?,
        val preferredColorOrder: String?
    )
    data class UserResponse(val id: UUID, val username: String, val fullName: String, val role: String, val requiresPasswordChange: Boolean) {
        companion object {
            fun from(u: User) = UserResponse(u.id, u.login, u.fullName, u.role.name, u.mustChangePassword)
        }
    }
}
