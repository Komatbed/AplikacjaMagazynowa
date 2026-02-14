package com.example.warehouse.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "user_preferences", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])])
data class UserPreferences(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(name = "favorite_profile_codes")
    var favoriteProfileCodes: String = "",
    
    @Column(name = "favorite_color_codes")
    var favoriteColorCodes: String = "",
    
    @Column(name = "preferred_profile_order")
    var preferredProfileOrder: String = "",
    
    @Column(name = "preferred_color_order")
    var preferredColorOrder: String = ""
)
