package com.kdob.piq.authserver.event

data class UserCreatedEvent(
    val authId: Long,
    val email: String,
    val roles: Set<String>
)