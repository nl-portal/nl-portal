/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.core.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.time.Instant
import java.util.Date

/**
 * Serves the JWKS of a test realm from a MockWebServer and signs tokens for it.
 */
object ActuatorJwks {
    private const val KEY_ID = "actuator-test"
    private const val JWKS_PATH = "/protocol/openid-connect/certs"

    private val realmKey: RSAKey = generateKey()

    // Same key id, different key: a token from another realm that happens to share the kid.
    private val otherRealmKey: RSAKey = generateKey()

    private val server: MockWebServer by lazy {
        MockWebServer().apply {
            dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        if (request.path == JWKS_PATH) {
                            MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setBody(JWKSet(realmKey.toPublicJWK()).toString())
                        } else {
                            MockResponse().setResponseCode(404)
                        }
                }
            start()
        }
    }

    val jwkSetUri: String
        get() = server.url(JWKS_PATH).toString()

    fun token(claims: Map<String, Any> = emptyMap()): String = sign(realmKey, claims)

    fun otherRealmToken(claims: Map<String, Any> = emptyMap()): String = sign(otherRealmKey, claims)

    fun realmRoles(vararg roles: String): Map<String, Any> = mapOf("realm_access" to mapOf("roles" to roles.toList()))

    private fun sign(
        key: RSAKey,
        claims: Map<String, Any>,
    ): String {
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .subject("admin")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .apply { claims.forEach { (name, value) -> claim(name, value) } }
                .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claimsSet)
            .apply { sign(RSASSASigner(key)) }
            .serialize()
    }

    private fun generateKey(): RSAKey = RSAKeyGenerator(2048).keyID(KEY_ID).generate()
}