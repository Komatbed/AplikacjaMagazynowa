package com.example.warehouse.dto

import com.example.warehouse.model.Role

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val role: Role
)

data class AuthResponse(
    val token: String,
    val username: String,
    val role: Role,
    val fullName: String,
    val requiresPasswordChange: Boolean
)
