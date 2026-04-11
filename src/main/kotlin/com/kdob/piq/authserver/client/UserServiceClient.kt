package com.kdob.piq.authserver.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

/**
 * Feign client for User Service.
 * Discovers user-service via Eureka — no hardcoded URL.
 */
@FeignClient(name = "user")
interface UserServiceClient {

    @GetMapping("/users/by-email")
    fun findByEmail(@RequestParam email: String): UserInfo

    @PostMapping("/users/federated")
    fun findOrCreateFederatedUser(@RequestBody request: CreateFederatedUserRequest): UserInfo
}

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String>
)

data class CreateFederatedUserRequest(
    val email: String,
    val name: String,
    val provider: String,
    val providerId: String
)