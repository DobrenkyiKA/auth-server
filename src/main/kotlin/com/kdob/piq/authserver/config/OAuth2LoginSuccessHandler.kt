package com.kdob.piq.authserver.config

import com.kdob.piq.authserver.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OAuth2LoginSuccessHandler(
    private val authService: AuthService,
    @Value("\${app.auth.default-redirect-uri:http://localhost:3000}") private val defaultRedirectUri: String
) : AuthenticationSuccessHandler {

    @Transactional
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken
        val oauthUser = oauthToken.principal
        val provider = oauthToken.authorizedClientRegistrationId

        val providerUserId = extractProviderUserId(oauthUser, provider)
        val email = extractEmail(oauthUser, provider)
            ?: throw IllegalStateException("Email not provided by $provider")

        val user = authService.findOrCreateUser(provider, providerUserId, email)
        val loginResult = authService.createLoginResult(user)

        response.addHeader("Set-Cookie", loginResult.refreshTokenCookie.toString())

        val redirectTo = request.getParameter("redirect_to") ?: defaultRedirectUri
        val separator = if (redirectTo.contains("?")) "&" else "?"
        response.sendRedirect("${defaultRedirectUri}/oauth2/callback${separator}token=${loginResult.accessToken}&redirect_to=$redirectTo")
    }

    private fun extractProviderUserId(oauthUser: OAuth2User?, provider: String): String {
        if (oauthUser == null) {
            throw IllegalStateException("oauthUser is null")
        }
        return when (provider) {
            "google" -> oauthUser.getAttribute<String>("sub")!!
            "github" -> oauthUser.getAttribute<Int>("id")!!.toString()
            "linkedin" -> oauthUser.getAttribute<String>("sub")!!
            else -> throw IllegalStateException("Unknown provider: $provider")
        }
    }

    private fun extractEmail(oauthUser: OAuth2User?, provider: String): String? {
        if (oauthUser == null) {
            return null
        }
        return when (provider) {
            "google" -> oauthUser.getAttribute("email")
            "github" -> oauthUser.getAttribute("email")
            "linkedin" -> oauthUser.getAttribute("email")
            else -> null
        }
    }
}