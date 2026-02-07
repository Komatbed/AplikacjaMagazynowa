package com.example.warehouse.service

import com.example.warehouse.dto.AuthResponse
import com.example.warehouse.dto.LoginRequest
import com.example.warehouse.dto.RegisterRequest
import com.example.warehouse.model.Role
import com.example.warehouse.model.User
import com.example.warehouse.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByLogin(request.username)) {
            throw RuntimeException("Username already exists")
        }
        val user = User(
            login = request.username,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            role = request.role
        )
        userRepository.save(user)
        val token = jwtService.generateToken(user)
        return AuthResponse(token, user.login, user.role, user.fullName)
    }

    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val user = userRepository.findByLogin(request.username)
            .orElseThrow()
        val token = jwtService.generateToken(user)
        return AuthResponse(token, user.login, user.role, user.fullName)
    }
    
    // Helper to init default admin if needed
    fun createDefaultAdmin() {
        if (!userRepository.existsByLogin("admin")) {
            val admin = User(
                login = "admin",
                passwordHash = passwordEncoder.encode("admin123"),
                fullName = "System Administrator",
                role = Role.ADMIN
            )
            userRepository.save(admin)
        }
    }
}
