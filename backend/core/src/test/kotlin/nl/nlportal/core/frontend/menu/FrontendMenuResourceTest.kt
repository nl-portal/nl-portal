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
package nl.nlportal.core.frontend.menu

import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class FrontendMenuResourceTest(
    @Autowired private val applicationContext: ApplicationContext,
) {
    @Test
    fun `get menu entries for the authenticated user`() {
        val webTestClient =
            WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockAuthentication(TestingAuthenticationToken("user", null, "ROLE_USER")))

        val responseBodyContent =
            webTestClient
                .get()
                .uri("/api/frontend/menu")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val responseJson = Mapper.get().readTree(responseBodyContent)

        assertEquals(1, responseJson.size())
        val entry = responseJson[0]
        assertEquals("thema-concept", entry.requiredAt("/name").stringValue())
        assertEquals("ParkingIcon", entry.requiredAt("/icon").stringValue())
        assertEquals(2, entry.requiredAt("/group").intValue())
        assertEquals(0, entry.requiredAt("/ordering").intValue())
        assertEquals("templated", entry.requiredAt("/route/type").stringValue())
        assertEquals("template-a", entry.requiredAt("/route/page").stringValue())
        assertEquals("/thema-concept", entry.requiredAt("/route/path").stringValue())
        assertFalse(entry.has("audience"))
    }
}