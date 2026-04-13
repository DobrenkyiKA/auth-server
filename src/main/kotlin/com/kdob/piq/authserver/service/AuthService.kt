package com.kdob.piq.authserver.service

import com.kdob.piq.authserver.domain.*
import com.kdob.piq.authserver.event.UserCreatedEvent
import com.kdob.piq.authserver.event.UserEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authUserRepository: AuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userEventPublisher: UserEventPublisher
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
                userId = saved.id!!,
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
}