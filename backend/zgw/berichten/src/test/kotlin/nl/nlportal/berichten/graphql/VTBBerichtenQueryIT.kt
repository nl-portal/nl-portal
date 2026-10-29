/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.berichten.graphql

import nl.nlportal.berichten.TestHelper
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.documentenapi.client.DocumentApisConfig
import nl.nlportal.vtbapi.autoconfigure.VTBModuleConfiguration
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.graphql.test.tester.HttpGraphQlTester
import tools.jackson.databind.JsonNode

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(Lifecycle.PER_METHOD)
class VTBBerichtenQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val vtbModuleConfiguration: VTBModuleConfiguration,
    @Autowired private val documentApisConfig: DocumentApisConfig,
) {
    lateinit var mockObjectenApi: MockWebServer

    @BeforeEach
    fun setUp() {
        mockObjectenApi = MockWebServer()
        setupMockObjectsApiServer()
        mockObjectenApi.start(port = 10001)
        vtbModuleConfiguration.properties.url = mockObjectenApi.url("/").toUri()
        documentApisConfig.properties.getConfig("zaken-main").url = mockObjectenApi.url("/").toString()
    }

    @AfterEach
    internal fun tearDown() {
        mockObjectenApi.shutdown()
    }

    @Test
    @WithBurgerUser("999990755")
    fun `should return VTBBerichtenPage`() {
        val responseBody =
            httpGraphQlTester
                .document(TestHelper.graphqlVTBBerichtenPageRequest)
                .execute()
                .errors()
                .verify()
                .path("getVTBBerichten")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Long text", responseBody.requiredAt("/content/0/berichtTekst")?.stringValue())
        assertEquals(null, responseBody.requiredAt("/content/0/geopendOp")?.stringValue())
        assertEquals("2026-11-27T10:22:33", responseBody.requiredAt("/content/0/publicatiedatum")?.stringValue())
        assertEquals("BETALEN", responseBody.requiredAt("/content/0/berichtType/handelingsPerspectief")?.stringValue())
    }

    @WithBurgerUser("999990755")
    @Test
    fun `should update bericht before return`() {
        val responseBody =
            httpGraphQlTester
                .document(TestHelper.graphqlValidVTBBerichtRequest)
                .execute()
                .errors()
                .verify()
                .path("getVTBBericht")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Long text", responseBody.get("berichtTekst").stringValue())
        assertEquals("2026-08-27T10:22:33", responseBody.get("publicatiedatum")?.stringValue())
        assertNotNull(responseBody.get("geopendOp").stringValue())
    }

    @WithBurgerUser("999990755")
    @Test
    fun `should return bericht`() {
        val responseBody =
            httpGraphQlTester
                .document(TestHelper.graphqlValidVTBBerichtReadRequest)
                .execute()
                .errors()
                .verify()
                .path("getVTBBericht")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Long text", responseBody.get("berichtTekst").stringValue())
        assertEquals("2026-08-27T10:22:33", responseBody.get("publicatiedatum")?.stringValue())
        assertNotNull(responseBody.get("geopendOp").stringValue())
    }

    @WithBurgerUser("999990755")
    @Test
    fun `should return bericht met documenten`() {
        val responseBody =
            httpGraphQlTester
                .document(TestHelper.graphqlValidVTBBerichtReadRequestWithDocuments)
                .execute()
                .errors()
                .verify()
                .path("getVTBBericht")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Long text", responseBody.get("berichtTekst").stringValue())
        assertEquals("2026-08-27T10:22:33", responseBody.get("publicatiedatum")?.stringValue())
        assertNotNull(responseBody.get("geopendOp").stringValue())
        assertEquals("095be615-a8ad-4c33-8e9c-c7612fbf6c9f", responseBody.requiredAt("/documenten/0/uuid")?.stringValue())
    }

    @WithBurgerUser("999990755")
    @Test
    fun `should return unopened berichten`() {
        val responseBody =
            httpGraphQlTester
                .document(TestHelper.graphqlUnopenedVTBBerichtenCountRequest)
                .execute()
                .errors()
                .verify()
                .path("getUnopenedVTBBerichtenCount")
                .entity(JsonNode::class.java)
                .get()

        assertEquals(3, responseBody.intValue())
    }

    fun setupMockObjectsApiServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val queryParams = request.path?.substringAfter('?')?.split('&') ?: emptyList()
                    val response =
                        when (request.method + " " + path) {
                            "GET /berichten/api/v1/berichten" -> {
                                if (queryParams.any { it.contains("ontvanger=urn:nld:brp:bsn:nummer:999990755") }) {
                                    TestHelper.mockResponse(TestHelper.vtbBerichtenResultPage)
                                } else {
                                    MockResponse().setResponseCode(404)
                                }
                            }

                            "GET /berichten/api/v1/berichten/9e021130-8cbd-4c6f-846a-677448e21ce8" -> {
                                TestHelper.mockResponse(TestHelper.vtbBerichtResponse)
                            }

                            "GET /berichten/api/v1/berichten/a4961c4a-29a7-4cc7-9d5d-bceed1dfccba" -> {
                                TestHelper.mockResponse(TestHelper.vtbBerichtResponseRead)
                            }

                            "PATCH /berichten/api/v1/berichten/9e021130-8cbd-4c6f-846a-677448e21ce8" -> {
                                TestHelper.mockResponse(TestHelper.vtbBerichtResponseRead)
                            }

                            "GET /berichten/api/v1/berichttypen/d9fabd4e-f6bd-43b5-a8ce-af412d7dd2e2" -> {
                                TestHelper.mockResponse(TestHelper.vtbBerichtTypeResponse)
                            }

                            "GET /enkelvoudiginformatieobjecten/095be615-a8ad-4c33-8e9c-c7612fbf6c9f" -> {
                                TestHelper.mockResponse(TestHelper.handleDocumentResponse)
                            }

                            else -> {
                                MockResponse().setResponseCode(404)
                            }
                        }
                    return response
                }
            }
        mockObjectenApi.dispatcher = dispatcher
    }
}