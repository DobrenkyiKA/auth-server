package com.kdob.piq.authserver.federation

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler

/**
 * Handles successful social login (Google, GitHub, LinkedIn).
 * After the OAuth2 provider authenticates the user, this handler
 * ensures the user exists in our User Service.
 */
class FederatedIdentityAuthenticationSuccessHandler(
    private val userHandler: UserRepositoryOAuth2UserHandler
) : AuthenticationSuccessHandler {

    private val delegate = SavedRequestAwareAuthenticationSuccessHandler()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        if (authentication is OAuth2AuthenticationToken) {
            val user = authentication.principal
            val provider = authentication.authorizedClientRegistrationId

            val email = when (user) {
                is OidcUser -> user.email
                is OAuth2User -> user.getAttribute<String>("email")
                else -> null
            }

            val name = when (user) {
                is OidcUser -> user.fullName ?: user.preferredUsername
                is OAuth2User -> user.getAttribute<String>("name")
                else -> null
            }

            if (email != null) {
                userHandler.handleUser(
                    email = email,
                    name = name,
                    provider = provider,
                    providerId = user.name
                )
            }
        }

        delegate.onAuthenticationSuccess(request, response, authentication)
    }
}