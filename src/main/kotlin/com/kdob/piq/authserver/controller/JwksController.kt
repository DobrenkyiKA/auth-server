package com.kdob.piq.authserver.controller

import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwksController(
    private val jwkSource: JWKSource<SecurityContext>
) {

    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> {
        val jwkSelector = JWKSelector(JWKMatcher.Builder().build())
        val keys = jwkSource.get(jwkSelector, null)
        return JWKSet(keys).toJSONObject()
    }
}