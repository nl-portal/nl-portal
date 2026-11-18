/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.ikoapi.portalpage

import kotlinx.coroutines.runBlocking
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.util.Mapper
import nl.nlportal.ikoapi.IkoMockServer
import nl.nlportal.ikoapi.IkoMockServer.Companion.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.JsonNode
import java.util.Base64
import java.util.Locale

@SpringBootTest
internal class IkoPortalPageBindingResolverTest(
    @Autowired private val resolver: IkoPortalPageBindingResolver,
) {
    @BeforeEach
    fun setUp() {
        server.reset()
        server.adpHandler = { request -> json("""{"bsn":"${request.requestUrl!!.queryParameter("id")}","vergunningen":[]}""") }
    }

    private fun owner() = context(OWNER_BSN)

    private fun params() = PortalPageParams(mapOf("profile" to "personen"))

    private fun garbageParams() =
        listOf(
            PortalPageParams(emptyMap()),
            PortalPageParams(mapOf("profile" to "personen", "page" to "1")),
            PortalPageParams(mapOf("profile" to "personen", "filter.status" to "open")),
            PortalPageParams(mapOf("profile" to "personen", "sort" to "naam:asc")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "page" to "-1")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "page" to "een")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "size" to "0")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "size" to "101")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "sort" to "naam")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "sort" to "naam:up")),
            PortalPageParams(mapOf("profile" to "personen", "container" to "c", "sort" to "naam:asc,datum")),
        )

    @Test
    fun `an outage throws`() {
        server.adpHandler = { json("""{"message":"down"}""", 500) }

        assertThrows<Exception> { runBlocking { resolver.resolve(owner(), params()) } }
    }

    @Test
    fun `output never contains identifiers of other subjects`() {
        val output = runBlocking { resolver.resolve(owner(), params()) }.data.toString()

        assertThat(output).doesNotContain(OTHER_BSN)
    }

    @Test
    fun `validate rejects garbage`() {
        garbageParams().forEach { params ->
            val problems =
                try {
                    resolver.validate(params)
                } catch (e: IllegalArgumentException) {
                    listOf(e.message)
                }
            assertTrue(problems.isNotEmpty(), "Expected problems for $params")
        }
    }

    @Test
    fun `sends the profile for the subject without container params`() {
        val data = runBlocking { resolver.resolve(owner(), params()) }.data

        val request = server.adpRequests().single()
        assertThat(request.requestUrl!!.encodedPath).endsWith("/personen")
        assertThat(request.requestUrl!!.queryParameterValues("id")).containsExactly(OWNER_BSN)
        assertThat(request.requestUrl!!.queryParameterValues("containerParam")).isEmpty()
        assertThat(data["bsn"].stringValue()).isEqualTo(OWNER_BSN)
    }

    @Test
    fun `maps page, size, sort and filters onto the container param`() {
        val params =
            PortalPageParams(
                mapOf(
                    "profile" to "personen",
                    "container" to "vergunningen",
                    "page" to "2",
                    "size" to "5",
                    "sort.0" to "startDatum:desc",
                    "sort.1" to "naam:asc",
                    "filter.status" to "open",
                    "filter.type" to "",
                ),
            )

        runBlocking { resolver.resolve(owner(), params) }

        val payload = containerParam()
        assertThat(payload["containerId"].stringValue()).isEqualTo("vergunningen")
        assertThat(payload["pageable"]["pageNumber"].intValue()).isEqualTo(2)
        assertThat(payload["pageable"]["pageSize"].intValue()).isEqualTo(5)
        assertThat(payload["pageable"]["sort"].toList().map { it["property"].stringValue() to it["direction"].stringValue() })
            .containsExactly("startDatum" to "DESC", "naam" to "ASC")
        assertThat(payload["filters"].propertyNames().toList()).containsExactly("status")
        assertThat(payload["filters"]["status"].stringValue()).isEqualTo("open")
    }

    @Test
    fun `reads sort from a comma-separated value`() {
        runBlocking {
            resolver.resolve(owner(), PortalPageParams(mapOf("profile" to "personen", "container" to "c", "sort" to "startDatum:desc, naam:ASC")))
        }

        assertThat(containerParam()["pageable"]["sort"].toList().map { it["property"].stringValue() to it["direction"].stringValue() })
            .containsExactly("startDatum" to "DESC", "naam" to "ASC")
    }

    @Test
    fun `defaults a container to the first page of 10 without filters`() {
        runBlocking { resolver.resolve(owner(), PortalPageParams(mapOf("profile" to "personen", "container" to "vergunningen"))) }

        val payload = containerParam()
        assertThat(payload["pageable"]["pageNumber"].intValue()).isEqualTo(0)
        assertThat(payload["pageable"]["pageSize"].intValue()).isEqualTo(10)
        assertThat(payload.has("filters")).isFalse()
    }

    @Test
    fun `a profile IKO doesn't find becomes an empty object`() {
        server.adpHandler = { json("""{"message":"not found"}""", 404) }

        assertThat(runBlocking { resolver.resolve(owner(), params()) }.data.isEmpty).isTrue()
    }

    @Test
    fun `validate accepts valid params`() {
        assertThat(
            resolver.validate(
                PortalPageParams(mapOf("profile" to "personen", "container" to "c", "page" to "0", "size" to "100", "sort" to "a.b:asc, c:DESC", "filter.x" to "y")),
            ),
        ).isEmpty()
    }

    @Test
    fun `by-id is not supported`() {
        assertThrows<UnsupportedOperationException> { runBlocking { resolver.resolve(owner(), params(), "id") } }
    }

    private fun containerParam(): JsonNode {
        val encoded =
            server
                .adpRequests()
                .single()
                .requestUrl!!
                .queryParameterValues("containerParam")
                .single()
        return Mapper.get().readTree(Base64.getDecoder().decode(encoded))
    }

    private fun context(bsn: String) = PortalPageRequestContext(JwtBuilder().aanvragerBsn(bsn).buildBurgerAuthentication(), Locale.forLanguageTag("nl"))

    companion object {
        private const val OWNER_BSN = "999993653"
        private const val OTHER_BSN = "999990676"
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
}