package com.kdob.piq.authserver.config

import com.kdob.piq.authserver.federation.FederatedIdentityAuthenticationSuccessHandler
import com.kdob.piq.authserver.federation.UserRepositoryOAuth2UserHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class DefaultSecurityConfig(
    private val userHandler: UserRepositoryOAuth2UserHandler
) {

    /**
     * Security filter chain for the login page and social login flows.
     * This handles the user-facing authentication experience.
     */
    @Bean
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val successHandler = FederatedIdentityAuthenticationSuccessHandler(userHandler)

        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login")
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(successHandler)
            }

        return http.build()
    }
}