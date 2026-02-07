package com.example.warehouse.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

enum class Role {
    ADMIN,
    KIEROWNIK,
    BRYGADZISTA,
    PRACOWNIK
}

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(unique = true, nullable = false)
    val login: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Column(nullable = false)
    val fullName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }

    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = login
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}
