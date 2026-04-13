package com.kdob.piq.authserver.service

import com.kdob.piq.authserver.domain.AuthUser
import com.kdob.piq.authserver.domain.RefreshToken
import com.kdob.piq.authserver.domain.RefreshTokenRepository
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    companion object {
        private val ACCESS_TOKEN_TTL = Duration.ofMinutes(30)
        private val REFRESH_TOKEN_TTL = Duration.ofDays(14)
    }

    fun generateAccessToken(user: AuthUser, issuerUri: String): String {
        val now = Instant.now()

        val claims = JwtClaimsSet.builder()
            .issuer(issuerUri)
            .issuedAt(now)
            .expiresAt(now.plus(ACCESS_TOKEN_TTL))
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("roles", user.roles.map { it.name })
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }

    @Transactional
    fun generateRefreshToken(userId: Long): String {
        val token = UUID.randomUUID().toString()

        refreshTokenRepository.save(
            RefreshToken(
                token = token,
                userId = userId,
                expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL)
            )
        )

        return token
    }

    @Transactional
    fun refreshAccessToken(refreshTokenValue: String, issuerUri: String, userLookup: (Long) -> AuthUser): String {
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow { IllegalArgumentException("Invalid refresh token") }

        if (refreshToken.revoked || refreshToken.isExpired()) {
            throw IllegalArgumentException("Refresh token has expired or been revoked")
        }

        val user = userLookup(refreshToken.userId)
        return generateAccessToken(user, issuerUri)
    }

    @Transactional
    fun revokeAllForUser(refreshTokenValue: String) {
        refreshTokenRepository.findByToken(refreshTokenValue)
            .ifPresent { rt ->
                refreshTokenRepository.findAllByUserId(rt.userId)
                    .forEach { it.revoke(); refreshTokenRepository.save(it) }
            }
    }
}