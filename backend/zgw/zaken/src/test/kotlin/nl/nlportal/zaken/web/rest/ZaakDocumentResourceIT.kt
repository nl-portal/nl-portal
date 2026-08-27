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
package nl.nlportal.zaken.web.rest

import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.core.util.Mapper
import nl.nlportal.zaken.TestHelper
import nl.nlportal.zakenapi.client.ZakenApiConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZaakDocumentResourceIT(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val zakenApiConfig: ZakenApiConfig,
) {
    lateinit var mockZakenApi: MockWebServer

    @BeforeEach
    fun setUp() {
        mockZakenApi = MockWebServer()
        mockZakenApi.start(8001)
        zakenApiConfig.properties.url = mockZakenApi.url("/").toString()
    }

    @AfterAll
    internal fun tearDown() {
        mockZakenApi.shutdown()
    }

    @Test
    @WithBurgerUser("111111110")
    fun `should return 401 for user without rol`() {
        // given
        val zaakDocumentResponse =
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(Mapper.get().writeValueAsString(TestHelper.testZaakDocument))
        val zaakRolResponse =
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                        "count": 0,
                        "results": []
                    }
                    """.trimIndent(),
                )
        with(mockZakenApi) {
            enqueue(zaakDocumentResponse)
            enqueue(zaakRolResponse)
        }

        // when
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/zakenapi/zaakdocument/${TestHelper.testZaakDocument.uuid}/content")
                    .build()
            }.exchange()
            .expectStatus()
            .isUnauthorized

        val zaakDocumentRequest = mockZakenApi.takeRequest()
        val zaakRolRequest = mockZakenApi.takeRequest()

        // then
        assertEquals(
            zaakDocumentRequest.requestUrl!!.encodedPath.substringAfterLast("/"),
            TestHelper.testZaakDocument.uuid,
        )
        assertEquals(
            zaakRolRequest.requestUrl!!.queryParameter("betrokkeneIdentificatie__natuurlijkPersoon__inpBsn"),
            "111111110",
        )
    }

    private fun getResourceContentByName(name: String): String? =
        this::class.java
            .getResourceAsStream(name)
            ?.readAllBytes()
            ?.decodeToString()
}