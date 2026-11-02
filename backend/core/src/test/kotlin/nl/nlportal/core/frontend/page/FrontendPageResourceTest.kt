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
package nl.nlportal.core.frontend.page

import nl.nlportal.core.frontend.page.FakePageDataResolver.Companion.json
import nl.nlportal.core.frontend.page.resolver.PageData
import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.Locale

@SpringBootTest
@Import(FrontendPageResourceTest.FakeResolverConfiguration::class)
@ExtendWith(OutputCaptureExtension::class)
class FrontendPageResourceTest(
    @Autowired private val applicationContext: ApplicationContext,
    @Autowired private val fakePageDataResolver: FakePageDataResolver,
) {
    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        fakePageDataResolver.contexts.clear()
        webTestClient =
            WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockAuthentication(TestingAuthenticationToken("user", null, "ROLE_USER")))
    }

    @Test
    fun `get a dynamic page with its file template and messages`() {
        val body =
            webTestClient
                .get()
                .uri("/api/frontend/pages/template-a")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        assertEquals(
            json(
                """
                {
                  "type": "dynamic",
                  "template": "{{ 'titel' | i18n }} {{ items }}\n",
                  "messages": { "nl-NL": { "titel": "Producten", "leeg": "Geen producten" }, "en-GB": { "titel": "Products" } },
                  "root": null,
                  "bindings": [
                    { "alias": "broken", "data": null, "error": true, "meta": { "params": {} } },
                    { "alias": "items", "data": { "page": "0", "status": null, "total": 23 }, "error": false,
                      "meta": { "params": { "page": "0" } } }
                  ]
                }
                """,
            ),
            Mapper.get().readTree(body),
        )
    }

    @Test
    fun `get a page for an id with its template from a binding`() {
        val body =
            webTestClient
                .get()
                .uri("/api/frontend/pages/template-a-detail/owned")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val response = Mapper.get().readTree(body)
        assertEquals("dynamic", response.requiredAt("/type").stringValue())
        assertEquals("detail", response.requiredAt("/root").stringValue())
        assertEquals("{{ naam }}", response.requiredAt("/template").stringValue())
        assertEquals(json("{}"), response.requiredAt("/messages"))
        assertEquals(json("""{"naam":"Erfpacht"}"""), response.requiredAt("/bindings/0/data"))
    }

    @Test
    fun `unknown, invisible and invalid pages and unknown ids are 404 without details`() {
        listOf(
            "/api/frontend/pages/onbekend",
            "/api/frontend/pages/remote-a",
            "/api/frontend/pages/remote-a/owned",
            "/api/frontend/pages/template-a-detail/someone-else",
        ).forEach { uri ->
            webTestClient
                .get()
                .uri(uri)
                .exchange()
                .expectStatus()
                .isNotFound
                .expectBody()
                .json("""{"message":"Page not found"}""", true)
        }
    }

    @Test
    fun `refetch accepts allowed params, also as numbers`() {
        val body =
            webTestClient
                .post()
                .uri("/api/frontend/pages/template-a/bindings/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"params":{"page":2,"filter.status":"actief"}}""")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val response = Mapper.get().readTree(body)
        assertEquals(json("""{"page":"2","status":"actief","total":23}"""), response.requiredAt("/data"))
        assertEquals(json("""{"page":"2","filter.status":"actief"}"""), response.requiredAt("/meta/params"))
    }

    @Test
    fun `refetch with a param outside the allowlist or an invalid value is 400`() {
        listOf("""{"params":{"source":"x"}}""", """{"params":{"page":-1}}""").forEach { request ->
            webTestClient
                .post()
                .uri("/api/frontend/pages/template-a/bindings/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""{"message":"Invalid params"}""", true)
        }
    }

    @Test
    fun `Accept-Language becomes the locale, nl by default`() {
        webTestClient
            .get()
            .uri("/api/frontend/pages/template-a")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB,en;q=0.9,nl;q=0.8")
            .exchange()
            .expectStatus()
            .isOk
        webTestClient
            .get()
            .uri("/api/frontend/pages/template-a")
            .exchange()
            .expectStatus()
            .isOk

        assertEquals(listOf(Locale.forLanguageTag("en-GB"), Locale.forLanguageTag("nl")), fakePageDataResolver.contexts.map { it.locale })
    }

    @Test
    fun `invalid page config is logged at startup without stopping it`(output: CapturedOutput) {
        assertTrue(output.all.contains("Invalid nl-portal.frontend.pages.remote-a: page requires a template"), output.all)
        assertTrue(output.all.contains("Invalid nl-portal.frontend.pages.template-a.bindings.broken: projection does not compile"))
        assertFalse(output.all.contains("pages.missing-template"))
        assertTrue(output.all.contains("Ignoring page template location 'ftp:ignored/'"))
        assertFalse(output.all.contains("pages.template-a-detail"))
        assertFalse(output.all.contains("pages.template-a:"))
    }

    @TestConfiguration
    class FakeResolverConfiguration {
        @Bean
        fun fakePageDataResolver() =
            FakePageDataResolver(
                objects = mapOf("owned" to PageData(json("""{"naam":"Erfpacht","eigenaren":["123"]}"""), template = "{{ naam }}")),
            )
    }
}