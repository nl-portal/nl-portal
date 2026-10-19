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
package nl.nlportal.openproduct.graphql

import tools.jackson.databind.JsonNode
import java.net.URI
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.openproduct.TestHelper
import nl.nlportal.openproduct.TestHelper.readFileAsString
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OpenProductActieQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val openProductModuleConfiguration: OpenProductModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
) {
    companion object {
        @JvmStatic
        var server: MockWebServer? = null

        @JvmStatic
        var url: String = ""

        @JvmStatic
        @DynamicPropertySource
        fun properties(propsRegistry: DynamicPropertyRegistry) {
            propsRegistry.add("nl-portal.config.openproduct.properties.product-type-api-url") { url }
            propsRegistry.add("nl-portal.config.openproduct.properties.product-api-url") { url }
            propsRegistry.add("nl-portal.config.objectenapi.properties.url") { url }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server = MockWebServer()
            server?.start(9000)
            url = server?.url("/").toString()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server?.shutdown()
        }
    }

    @BeforeEach
    internal fun setUp() {
        setupMockServer()
        url = server?.url("/").toString()
        openProductModuleConfiguration.properties.productTypeApiUrl = URI(url)
        openProductModuleConfiguration.properties.productApiUrl = URI(url)
        objectsApiClientConfig.properties.url = URI(url)
    }

    @Test
    @WithBurgerUser("569312863")
    fun `get acties`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductActies.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductActies")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(1, responseBody.get("number")?.intValue())
            assertEquals("watkanikregelen-belastingen", responseBody.requiredAt("/content/0/naam")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get actie`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductActie.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductActie")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals("watkanikregelen-belastingen", responseBody.requiredAt("/naam")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get actie decisions`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductActieDecision.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductActieDecision")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals("https://openformulieren-zgw.test.denhaag.nl/bezwaarschrift-overige-gemeentelijke-belastingen/startpagina", responseBody.requiredAt("/0/action/value")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get actie prefill`() {
        val responseBody =
            httpGraphQlTester
                .document(readFileAsString("/config/graphql/getOpenProductActiePrefill.gql"))
                .execute()
                .errors()
                .verify()
                .path("getOpenProductActiePrefill")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("f9d7f166-bcea-4448-a984-4e717e558458", responseBody.requiredAt("/objectId")?.stringValue())
        assertEquals("http://localhost:9000/engine-rest/decision-definition/key/alg-belastingen", responseBody.requiredAt("/formulierUrl")?.stringValue())
    }

    @Test
    @WithBurgerUser("569312864")
    fun `get actie prefill not authorized for product`() {
        httpGraphQlTester
            .document(readFileAsString("/config/graphql/getOpenProductActiePrefill.gql"))
            .execute()
            .errors()
            .satisfy { errors ->
                assertEquals(1, errors.size)
                assertTrue(errors[0].message!!.contains("401 UNAUTHORIZED \"Not authorized\""))
            }
    }

    private fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "GET /acties/2435b986-7742-4cef-91f2-e1162c2f19c9" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-actie.json")
                            }

                            "GET /acties" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-acties.json")
                            }

                            "GET /producten/694242af-d906-470b-b7e1-eb3527886854" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-product.json")
                            }

                            "POST /engine-rest/decision-definition/key/alg-belastingen/evaluate" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-dmn-decision.json")
                            }

                            "POST /api/v2/objects" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-prefill-object.json")
                            }

                            else -> {
                                MockResponse().setResponseCode(404)
                            }
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}