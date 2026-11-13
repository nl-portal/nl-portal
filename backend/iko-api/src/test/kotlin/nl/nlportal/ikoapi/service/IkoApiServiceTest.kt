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
package nl.nlportal.ikoapi.service

import kotlinx.coroutines.runBlocking
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.core.util.Mapper
import nl.nlportal.ikoapi.IkoMockServer
import nl.nlportal.ikoapi.IkoMockServer.Companion.json
import nl.nlportal.ikoapi.domain.IkoContainerParam
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.Base64
import java.util.UUID

@SpringBootTest
internal class IkoApiServiceTest(
    @Autowired private val service: IkoApiService,
) {
    companion object {
        private val server = IkoMockServer()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val properties = server.properties()
            registry.add("nl-portal.config.ikoapi.properties.url") { properties.url.toString() }
            registry.add("nl-portal.config.ikoapi.properties.token-url") { properties.tokenUrl.toString() }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.shutdown()
        }
    }

    @BeforeEach
    fun setUp() {
        server.reset()
    }

    @Test
    fun `uses the BSN of a burger as id`() {
        runBlocking { service.getAggregatedDataProfile(JwtBuilder().aanvragerBsn("999993653").buildBurgerAuthentication(), "personen") }

        assertThat(adpRequest().requestUrl!!.queryParameterValues("id")).containsExactly("999993653")
    }

    @Test
    fun `uses the KVK number of a bedrijf as id`() {
        runBlocking { service.getAggregatedDataProfile(JwtBuilder().aanvragerKvk("12345678").buildBedrijfAuthentication(), "personen") }

        assertThat(adpRequest().requestUrl!!.queryParameterValues("id")).containsExactly("12345678")
    }

    @Test
    fun `returns null for a keycloak user without calling IKO`() {
        val result =
            runBlocking {
                service.getAggregatedDataProfile(
                    JwtBuilder().aanvragerUid(UUID.randomUUID()).buildKeycloakUserAuthentication(),
                    "personen",
                )
            }

        assertThat(result).isNull()
        assertThat(server.adpRequests()).isEmpty()
    }

    @Test
    fun `rejects a profile outside the allowlist without calling IKO`() {
        assertThrows<IkoApiException> {
            runBlocking { service.getAggregatedDataProfile(burger(), "other") }
        }

        assertThat(server.adpRequests()).isEmpty()
    }

    @Test
    fun `encodes container params as standard Base64 JSON that IKO can decode`() {
        val containerParam =
            IkoContainerParam(
                containerId = "zaken",
                pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("identificatie"))),
                filters = mapOf("zaaktype" to "x"),
            )

        runBlocking { service.getAggregatedDataProfile(burger(), "personen", listOf(containerParam)) }

        val encoded = adpRequest().requestUrl!!.queryParameterValues("containerParam").single()
        val json = Base64.getDecoder().decode(encoded).decodeToString()
        val tree = Mapper.get().readTree(json)
        assertThat(tree["containerId"].asString()).isEqualTo("zaken")
        assertThat(tree["pageable"]["pageNumber"].isInt).isTrue()
        assertThat(tree["pageable"]["pageNumber"].intValue()).isEqualTo(0)
        assertThat(tree["pageable"]["pageSize"].isInt).isTrue()
        assertThat(tree["pageable"]["pageSize"].intValue()).isEqualTo(10)
        assertThat(tree["pageable"]["sort"].isArray).isTrue()
        assertThat(tree["pageable"]["sort"][0]["property"].asString()).isEqualTo("identificatie")
        assertThat(tree["pageable"]["sort"][0]["direction"].asString()).isEqualTo("ASC")
        assertThat(tree["filters"]["zaaktype"].asString()).isEqualTo("x")
        assertThat(json).doesNotContain("null")
    }

    @Test
    fun `omits an absent pageable and filters from the encoded JSON`() {
        runBlocking { service.getAggregatedDataProfile(burger(), "personen", listOf(IkoContainerParam("zaken"))) }

        val encoded = adpRequest().requestUrl!!.queryParameterValues("containerParam").single()
        assertThat(Base64.getDecoder().decode(encoded).decodeToString()).isEqualTo("""{"containerId":"zaken"}""")
    }

    @Test
    fun `returns the body on 200 as JsonNode`() {
        server.adpResponse = { json("""{"persoon":{"bsn":"999993653"}}""") }

        val result = runBlocking { service.getAggregatedDataProfile(burger(), "personen") }

        assertThat(result).isEqualTo(Mapper.get().readTree("""{"persoon":{"bsn":"999993653"}}"""))
    }

    @Test
    fun `returns null on 404`() {
        server.adpResponse = { json("""{"message":"Profile not found","correlationId":"c-404"}""", status = 404) }

        val result = runBlocking { service.getAggregatedDataProfile(burger(), "personen") }

        assertThat(result).isNull()
    }

    @Test
    fun `maps 401 to IkoApiException with correlationId but without IKO's message`() {
        server.adpResponse = { json("""{"message":"secret detail","correlationId":"c-401"}""", status = 401) }

        val ex =
            assertThrows<IkoApiException> {
                runBlocking { service.getAggregatedDataProfile(burger(), "personen") }
            }

        assertThat(ex.message).contains("c-401").doesNotContain("secret detail")
    }

    @Test
    fun `maps 500 to IkoApiException with correlationId but without IKO's message`() {
        server.adpResponse = { json("""{"message":"secret detail","correlationId":"c-500"}""", status = 500) }

        val ex =
            assertThrows<IkoApiException> {
                runBlocking { service.getAggregatedDataProfile(burger(), "personen") }
            }

        assertThat(ex.message).contains("c-500").doesNotContain("secret detail")
    }

    @Test
    fun `maps a non-JSON error body to IkoApiException without correlationId`() {
        server.adpResponse = {
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/plain")
                .setBody("Internal Server Error")
        }

        val ex =
            assertThrows<IkoApiException> {
                runBlocking { service.getAggregatedDataProfile(burger(), "personen") }
            }

        assertThat(ex.message).contains("correlationId=null")
    }

    private fun burger() = JwtBuilder().aanvragerBsn("999993653").buildBurgerAuthentication()

    private fun adpRequest(): RecordedRequest = server.adpRequests().single()
}