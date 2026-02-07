package com.example.warehouse.repository

import com.example.warehouse.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByLogin(login: String): Optional<User>
    fun existsByLogin(login: String): Boolean
}
