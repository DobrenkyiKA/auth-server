package com.kdob.piq.authserver.federation

import com.kdob.piq.authserver.client.UserServiceClient
import com.kdob.piq.authserver.client.CreateFederatedUserRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Ensures a user exists in the User Service after social login.
 * If the user doesn't exist, creates them.
 */
@Component
class UserRepositoryOAuth2UserHandler(
    private val userServiceClient: UserServiceClient
) {

    private val logger = LoggerFactory.getLogger(UserRepositoryOAuth2UserHandler::class.java)

    fun handleUser(email: String, name: String?, provider: String, providerId: String) {
        try {
            userServiceClient.findOrCreateFederatedUser(
                CreateFederatedUserRequest(
                    email = email,
                    name = name ?: email.substringBefore("@"),
                    provider = provider,
                    providerId = providerId
                )
            )
            logger.info("User synced: email={}, provider={}", email, provider)
        } catch (e: Exception) {
            logger.error("Failed to sync user: email={}, provider={}", email, provider, e)
        }
    }
}