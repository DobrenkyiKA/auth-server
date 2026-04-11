package com.kdob.piq.authserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import java.time.Duration
import java.util.*

@Configuration
class AuthorizationServerConfig {


//Security filter chain for OAuth2 Authorization Server endpoints:
///oauth2/authorize, /oauth2/token, /oauth2/jwks, /.well-known/*, etc.

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity
    ): SecurityFilterChain {
        val authorizationServerConfigurer =
            OAuth2AuthorizationServerConfigurer()

        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(authorizationServerConfigurer) { authServer ->
                authServer.oidc(Customizer.withDefaults())
            }
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            }

        return http.build()
    }

    /**
     * Registered OAuth2 clients.
     *
     * For MVP, we define clients in memory.
     * In production, move to JdbcRegisteredClientRepository backed by PostgreSQL.
     */
    @Bean
    fun registeredClientRepository(): RegisteredClientRepository {

// Public client for frontend apps (uses PKCE, no client secret)
        val frontendClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("interviewprep-frontend")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/interviewprep")
            .redirectUri("http://localhost:3000/api/auth/callback")
            .postLogoutRedirectUri("http://localhost:8080/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(15))
                    .refreshTokenTimeToLive(Duration.ofDays(7))
                    .reuseRefreshTokens(false)
                    .build()
            )
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(true) // PKCE required
                    .build()
            )
            .build()

// Confidential client for service-to-service communication
        val serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("interviewprep-service")
            .clientSecret("{noop}service-secret-change-in-production")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("service:read")
            .scope("service:write")
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(5))
                    .build()
            )
            .build()

        return InMemoryRegisteredClientRepository(frontendClient, serviceClient)
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .issuer("http://localhost:8081") // Overridden per environment via config
            .build()
}