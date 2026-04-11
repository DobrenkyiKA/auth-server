package com.kdob.piq.authserver.config

import com.kdob.piq.authserver.client.UserServiceClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

@Configuration
class TokenCustomizerConfig(
    private val userServiceClient: UserServiceClient
) {

    private val logger = LoggerFactory.getLogger(TokenCustomizerConfig::class.java)

    /**
     * Customizes the JWT access token with user-specific claims:
     * - email: user's email address
     * - roles: user's roles from User Service
     *
     * These claims are read by the Gateway and forwarded as headers
     * to downstream services.
     */
    @Bean
    fun tokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (context.tokenType.value == "access_token") {
                val principal: Authentication = context.getPrincipal()
                val email = extractEmail(principal)

                if (email != null) {
                    try {
                        val userInfo = userServiceClient.findByEmail(email)
                        context.claims.claims { claims ->
                            claims["email"] = userInfo.email
                            claims["roles"] = userInfo.roles
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to fetch user info for email: $email", e)
                        context.claims.claims { claims ->
                            claims["email"] = email
                            claims["roles"] = listOf("USER")
                        }
                    }
                }
            }
        }

    private fun extractEmail(principal: Authentication): String? {
        return when (val user = principal.principal) {
            is OidcUser -> user.email
            is OAuth2User -> user.getAttribute("email")
            else -> principal.name
        }
    }
}