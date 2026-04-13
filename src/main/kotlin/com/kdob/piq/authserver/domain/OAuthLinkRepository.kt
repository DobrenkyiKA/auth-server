package com.kdob.piq.authserver.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OAuthLinkRepository : JpaRepository<OAuthLink, Long> {
    fun findByProviderAndProviderUserId(provider: String, providerUserId: String): Optional<OAuthLink>
}