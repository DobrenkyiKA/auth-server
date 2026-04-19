package com.kdob.piq.authserver.service

import com.kdob.piq.authserver.domain.*
import com.kdob.piq.authserver.event.UserCreatedEvent
import com.kdob.piq.authserver.event.UserEventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class AuthService(
    private val authUserRepository: AuthUserRepository,
    private val oAuthLinkRepository: OAuthLinkRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val userEventPublisher: UserEventPublisher,
    @Value("\${app.auth.issuer-uri}") private val issuerUri: String
) {

    @Transactional
    fun register(email: String, rawPassword: String): AuthUser {
        if (authUserRepository.existsByEmail(email)) {
            throw IllegalStateException("User with email [$email] already exists")
        }

        val user = AuthUser(
            email = email,
            passwordHash = passwordEncoder.encode(rawPassword),
            roles = mutableSetOf(Role.USER)
        )

        val saved = authUserRepository.save(user)

        userEventPublisher.publishUserCreated(
            UserCreatedEvent(
                authId = saved.id!!,
                email = saved.email,
                roles = saved.roles.map { it.name }.toSet()
            )
        )

        return saved
    }

    fun authenticate(email: String, rawPassword: String): AuthUser {
        val user = authUserRepository.findByEmail(email)
            .orElseThrow { IllegalStateException("Invalid credentials") }

        if (user.passwordHash == null || !passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw IllegalStateException("Invalid credentials")
        }

        return user
    }

    fun findById(id: Long): AuthUser {
        return authUserRepository.findById(id)
            .orElseThrow { IllegalStateException("User not found") }
    }

    @Transactional
    fun findOrCreateUser(provider: String, providerUserId: String, email: String): AuthUser {
        // Check if OAuth link already exists
        val existingLink = oAuthLinkRepository.findByProviderAndProviderUserId(provider, providerUserId)
        if (existingLink.isPresent) {
            return existingLink.get().user
        }

        // Check if user with this email already exists (link new provider)
        val existingUser = authUserRepository.findByEmail(email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            val link = OAuthLink(provider = provider, providerUserId = providerUserId, user = user)
            user.oauthLinks.add(link)
            authUserRepository.save(user)
            return user
        }

        // Create new user
        val newUser = AuthUser(email = email, roles = mutableSetOf(Role.USER))
        val savedUser = authUserRepository.save(newUser)

        val link = OAuthLink(provider = provider, providerUserId = providerUserId, user = savedUser)
        savedUser.oauthLinks.add(link)
        authUserRepository.save(savedUser)

        userEventPublisher.publishUserCreated(
            UserCreatedEvent(
                authId = savedUser.id!!,
                email = savedUser.email,
                roles = savedUser.roles.map { it.name }.toSet()
            )
        )

        return savedUser
    }

    fun createLoginResult(user: AuthUser): LoginResult {
        val accessToken = tokenService.generateAccessToken(user, issuerUri)
        val refreshToken = tokenService.generateRefreshToken(user.id!!)

        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(14))
            .build()

        return LoginResult(accessToken, cookie)
    }
}

data class LoginResult(
    val accessToken: String,
    val refreshTokenCookie: ResponseCookie
)