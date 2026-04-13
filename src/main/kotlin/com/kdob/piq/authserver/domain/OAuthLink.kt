package com.kdob.piq.authserver.domain

import jakarta.persistence.*

@Entity
@Table(
    name = "oauth_links",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "providerUserId"])]
)
class OAuthLink(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oauth_links_seq")
    @SequenceGenerator(name = "oauth_links_seq", sequenceName = "oauth_links_id_seq", allocationSize = 50)
    val id: Long? = null,

    @Column(nullable = false)
    val provider: String,

    @Column(nullable = false)
    val providerUserId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AuthUser
)