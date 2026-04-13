package com.kdob.piq.authserver.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "auth_users")
class AuthUser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_users_seq")
    @SequenceGenerator(name = "auth_users_seq", sequenceName = "auth_users_id_seq", allocationSize = 50)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column
    var passwordHash: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "auth_user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val roles: MutableSet<Role> = mutableSetOf(Role.USER),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val oauthLinks: MutableList<OAuthLink> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)