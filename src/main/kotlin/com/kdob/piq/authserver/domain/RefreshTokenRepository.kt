package com.kdob.piq.authserver.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun findAllByUserId(userId: Long): List<RefreshToken>
}