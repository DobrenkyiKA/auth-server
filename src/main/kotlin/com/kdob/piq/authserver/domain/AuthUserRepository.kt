package com.kdob.piq.authserver.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AuthUserRepository : JpaRepository<AuthUser, Long> {
    fun findByEmail(email: String): Optional<AuthUser>
    fun existsByEmail(email: String): Boolean
}