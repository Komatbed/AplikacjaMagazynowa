package com.example.warehouse.data.model

data class InventoryItemDto(
    val id: String,
    val location: LocationDto,
    val profileCode: String,
    val internalColor: String = "UNKNOWN",
    val externalColor: String = "UNKNOWN",
    val coreColor: String? = null,
    val lengthMm: Int,
    val quantity: Int,
    val status: String,
    val reservedBy: String? = null,
    val reservationDate: String? = null
)

data class LocationDto(
    val id: Long,
    val rowNumber: Int,
    val paletteNumber: Int,
    val label: String
)

data class LocationStatusDto(
    val id: Int,
    val label: String?,
    val rowNumber: Int,
    val paletteNumber: Int,
    val isWaste: Boolean,
    val itemCount: Int,
    val capacity: Int = 50,
    val profileCodes: List<String>?,
    val coreColors: List<String>? = null
)

data class IssueReportRequest(
    val description: String,
    val profileCode: String? = null,
    val locationLabel: String? = null
)

// Config definitions are in ConfigModels.kt

// Optimization
// OptimizationRequest and WasteRecommendationResponse are in OptimizationModels.kt

data class UserDto(
    val id: String,
    val username: String,
    val fullName: String?,
    val role: String,
    val requiresPasswordChange: Boolean
)

data class UserPreferencesDto(
    val favoriteProfileCodes: String,
    val favoriteColorCodes: String,
    val preferredProfileOrder: String,
    val preferredColorOrder: String
)

data class UserCreateRequest(
    val username: String,
    val password: String,
    val fullName: String?,
    val role: String
)

data class PasswordResetRequest(
    val newPassword: String
)

data class RoleChangeRequest(
    val role: String
)
