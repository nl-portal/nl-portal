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
package nl.nlportal.ikoapi.client

import kotlinx.coroutines.runBlocking
import nl.nlportal.ikoapi.IkoMockServer
import nl.nlportal.ikoapi.IkoMockServer.Companion.json
import nl.nlportal.ikoapi.IkoMockServer.Companion.token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.util.Base64

class IkoApiClientTest {
    private lateinit var server: IkoMockServer
    private lateinit var client: IkoApiClient

    @BeforeEach
    fun setUp() {
        server = IkoMockServer()
        client = server.client()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends name as path segment and id as query param`() {
        runBlocking { client.getAggregatedDataProfile("personen", "999993653") }

        val request = server.adpRequests().single()
        assertThat(request.path).isEqualTo("/api/iko/aggregated-data-profiles/personen?id=999993653")
        assertThat(request.requestUrl!!.queryParameterValues("id")).containsExactly("999993653")
        assertThat(request.requestUrl!!.queryParameterValues("containerParam")).isEmpty()
    }

    @Test
    fun `encodes a name with a space as one path segment`() {
        runBlocking { client.getAggregatedDataProfile("mijn profiel", "1") }

        assertThat(server.adpRequests().single().path).startsWith("/api/iko/aggregated-data-profiles/mijn%20profiel?")
    }

    @Test
    fun `strictly encodes repeated containerParam values`() {
        runBlocking { client.getAggregatedDataProfile("personen", "1", listOf("a+b/c==", "x")) }

        val request = server.adpRequests().single()
        assertThat(request.path).contains("containerParam=a%2Bb%2Fc%3D%3D").contains("containerParam=x")
        assertThat(request.requestUrl!!.queryParameterValues("containerParam")).containsExactly("a+b/c==", "x")
    }

    @Test
    fun `returns an object body as an ObjectNode`() {
        server.adpResponse = { json("""{"persoon":{"bsn":"999993653"}}""") }

        val result = runBlocking { client.getAggregatedDataProfile("personen", "999993653") }

        assertThat(result).isInstanceOf(ObjectNode::class.java)
        assertThat(result!!["persoon"]["bsn"].asString()).isEqualTo("999993653")
    }

    @Test
    fun `returns an array body as an ArrayNode`() {
        server.adpResponse = { json("""["a","b"]""") }

        val result = runBlocking { client.getAggregatedDataProfile("personen", "1") }

        assertThat(result).isInstanceOf(ArrayNode::class.java)
        assertThat(result!!.size()).isEqualTo(2)
        assertThat(result[0].asString()).isEqualTo("a")
        assertThat(result[1].asString()).isEqualTo("b")
    }

    @Test
    fun `raises a 404 as WebClientResponseException with readable body`() {
        server.adpResponse = { json("""{"message":"nope","correlationId":"c-1"}""", status = 404) }

        val ex =
            assertThrows<WebClientResponseException.NotFound> {
                runBlocking { client.getAggregatedDataProfile("personen", "1") }
            }

        assertThat(ex.responseBodyAsString).contains("\"message\":\"nope\"").contains("\"correlationId\":\"c-1\"")
    }

    @Test
    fun `fetches the token once and reuses it`() {
        runBlocking {
            client.getAggregatedDataProfile("personen", "1")
            client.getAggregatedDataProfile("personen", "2")
        }

        assertThat(server.adpRequests()).hasSize(2).allSatisfy {
            assertThat(it.getHeader("Authorization")).isEqualTo("Bearer t-1")
        }
        assertThat(server.tokenRequests()).hasSize(1)
    }

    @Test
    fun `requests the token with client credentials and basic auth`() {
        runBlocking { client.getAggregatedDataProfile("personen", "1") }

        val request = server.tokenRequests().single()
        val basic = Base64.getEncoder().encodeToString("nl-portal:secret".toByteArray())
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.body.readUtf8()).contains("grant_type=client_credentials")
        assertThat(request.getHeader("Authorization")).isEqualTo("Basic $basic")
    }

    @Test
    fun `refetches a token that expires within the clock skew`() {
        server.tokenResponse = { token(expiresIn = 30) }

        runBlocking {
            client.getAggregatedDataProfile("personen", "1")
            client.getAggregatedDataProfile("personen", "2")
        }

        assertThat(server.tokenRequests()).hasSize(2)
    }

    @Test
    fun `fails without calling IKO when the token endpoint rejects the client`() {
        server.tokenResponse = { json("""{"error":"unauthorized_client"}""", status = 401) }

        assertThrows<Exception> {
            runBlocking { client.getAggregatedDataProfile("personen", "1") }
        }

        assertThat(server.adpRequests()).isEmpty()
    }
}