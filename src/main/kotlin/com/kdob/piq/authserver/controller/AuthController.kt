package com.kdob.piq.authserver.controller

import com.kdob.piq.authserver.service.AuthService
import com.kdob.piq.authserver.service.TokenService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
    @Value("\${app.auth.issuer-uri}") private val issuerUri: String
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest) {
        authService.register(request.email, request.password)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val user = authService.authenticate(request.email, request.password)
        val loginResult = authService.createLoginResult(user)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, loginResult.refreshTokenCookie.toString())
            .body(TokenResponse(loginResult.accessToken))
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?
    ): ResponseEntity<TokenResponse> {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val newAccessToken = tokenService.refreshAccessToken(refreshToken, issuerUri) { userId ->
            authService.findById(userId)
        }

        return ResponseEntity.ok(TokenResponse(newAccessToken))
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "refreshToken", required = false) token: String?
    ): ResponseEntity<Void> {
        if (token != null) {
            tokenService.revokeAllForUser(token)
        }

        val deleteCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
            .build()
    }
}

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class TokenResponse(val accessToken: String)