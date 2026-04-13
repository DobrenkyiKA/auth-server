package com.kdob.piq.authserver.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_tokens_seq")
    @SequenceGenerator(name = "refresh_tokens_seq", sequenceName = "refresh_tokens_id_seq", allocationSize = 50)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun revoke() {
        revoked = true
    }
}