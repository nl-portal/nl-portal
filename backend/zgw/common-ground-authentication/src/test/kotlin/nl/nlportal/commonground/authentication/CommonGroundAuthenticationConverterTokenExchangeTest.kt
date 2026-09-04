/*
 * Copyright 2015-2023 Ritense BV, the Netherlands.
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
package nl.nlportal.commonground.authentication

import nl.nlportal.commonground.authentication.KeycloakConfig.KeycloakCredentials
import nl.nlportal.commonground.authentication.KeycloakConfig.TokenExchangeVersion
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.net.URLDecoder
import kotlin.test.assertFailsWith

internal class CommonGroundAuthenticationConverterTokenExchangeTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var issuer: String
    private val decoder: ReactiveJwtDecoder = mock()

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        issuer = "http://${mockServer.hostName}:${mockServer.port}/auth/realms/nlportal"
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `v1 sends the audience and no subject_token_type`() {
        val request = exchangeAndRecord(keycloakConfig(TokenExchangeVersion.V1, "gzac-portal-token-exchange"))
        val form = request.parseFormBody()

        assertEquals("/auth/realms/nlportal/protocol/openid-connect/token", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/x-www-form-urlencoded"))
        assertEquals("gzac-portal-m2m", form["client_id"])
        assertEquals("secret", form["client_secret"])
        assertEquals("urn:ietf:params:oauth:grant-type:token-exchange", form["grant_type"])
        assertEquals("token", form["subject_token"])
        assertEquals("urn:ietf:params:oauth:token-type:access_token", form["requested_token_type"])
        assertEquals("gzac-portal-token-exchange", form["audience"])
        assertNull(form["subject_token_type"])
    }

    @Test
    fun `v1 keeps the parameter order of the original implementation`() {
        val request = exchangeAndRecord(keycloakConfig(TokenExchangeVersion.V1, "gzac-portal-token-exchange"))

        assertEquals(
            listOf(
                "client_id",
                "client_secret",
                "grant_type",
                "subject_token",
                "requested_token_type",
                "audience",
            ),
            request.parseFormBody().keys.toList(),
        )
    }

    @Test
    fun `v2 sends subject_token_type and omits the audience when it is not configured`() {
        val request = exchangeAndRecord(keycloakConfig(TokenExchangeVersion.V2, null))
        val form = request.parseFormBody()

        assertEquals("urn:ietf:params:oauth:token-type:access_token", form["subject_token_type"])
        assertNull(form["audience"])
    }

    @Test
    fun `v2 sends the audience when one is configured`() {
        val request = exchangeAndRecord(keycloakConfig(TokenExchangeVersion.V2, "some-other-client"))
        val form = request.parseFormBody()

        assertEquals("urn:ietf:params:oauth:token-type:access_token", form["subject_token_type"])
        assertEquals("some-other-client", form["audience"])
    }

    @Test
    fun `v2 omits a blank audience`() {
        val request = exchangeAndRecord(keycloakConfig(TokenExchangeVersion.V2, " "))

        assertNull(request.parseFormBody()["audience"])
    }

    @Test
    fun `v1 without an audience fails before any request is made`() {
        val converter = CommonGroundAuthenticationConverter(decoder, keycloakConfig(TokenExchangeVersion.V1, null))

        val exception =
            assertFailsWith<IllegalArgumentException> {
                converter.tokenExchange(jwt()).block()
            }

        assertEquals(
            "nl-portal.authentication.keycloak.audience is required when token-exchange-version is v1",
            exception.message,
        )
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun `an error response from keycloak propagates instead of yielding a token`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_request","error_description":"Requested audience not available"}"""),
        )
        val converter = CommonGroundAuthenticationConverter(decoder, keycloakConfig(TokenExchangeVersion.V2, null))

        assertFailsWith<Exception> { converter.tokenExchange(jwt()).block() }
    }

    private fun exchangeAndRecord(keycloakConfig: KeycloakConfig): RecordedRequest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"access_token":"exchanged-token"}"""),
        )
        val converter = CommonGroundAuthenticationConverter(decoder, keycloakConfig)

        val response = converter.tokenExchange(jwt()).block()

        assertEquals("exchanged-token", response?.accessToken)
        return mockServer.takeRequest()
    }

    private fun keycloakConfig(
        version: TokenExchangeVersion,
        audience: String?,
    ) = KeycloakConfig(
        resource = "gzac-portal-m2m",
        audience = audience,
        credentials = KeycloakCredentials("secret"),
        tokenExchangeVersion = version,
    )

    private fun jwt(): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "none")
            .claim("iss", issuer)
            .build()

    private fun RecordedRequest.parseFormBody(): Map<String, String> =
        body
            .readUtf8()
            .split("&")
            .filter { it.isNotBlank() }
            .associate { parameter ->
                val (name, value) = parameter.split("=", limit = 2)
                URLDecoder.decode(name, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
}