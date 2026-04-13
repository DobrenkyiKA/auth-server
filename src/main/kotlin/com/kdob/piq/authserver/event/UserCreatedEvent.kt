package com.kdob.piq.authserver.event

data class UserCreatedEvent(
    val userId: Long,
    val email: String,
    val roles: Set<String>
)