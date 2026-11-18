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
package nl.nlportal.zgw.taak.graphql

import java.net.URI
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import nl.nlportal.zgw.taak.TestHelper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.JsonNode

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class TaakPaymentMutationIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
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
            propsRegistry.add("nl-portal.config.payment.direct.properties.url") { url }
            propsRegistry.add("nl-portal.config.objectenapi.properties.url") { url }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server = MockWebServer()
            server?.start()
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
        directPaymentModuleConfiguration.properties.url = url
        objectsApiClientConfig.properties.url = URI(url)
    }

    @Test
    @WithBurgerUser("569312863")
    fun doDirectPaymentWithIdentifier() {
        val mutation =
            """
            mutation {
                doTaakPayment(
                    paymentRequest: { 
                        identifier: "belastingzaken", 
                        taakId: "58fad5ab-dc2f-11ec-9075-f22a405ce708"
                    }
                ) {
                redirectUrl,
                }
            }
            """.trimIndent()

        val responseBody =
            httpGraphQlTester
                .document(mutation)
                .execute()
                .errors()
                .verify()
                .path("doTaakPayment")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("https://payment.preprod.direct.worldline-solutions.com/hostedcheckout/PaymentMethods/Selection/e61340e579e04172a740676ffdda162e", responseBody.get("redirectUrl")?.stringValue())
    }

    @Test
    @WithBurgerUser("569312863")
    fun doDirectPaymentWithIdentifierAndRefererHeader() {
        val mutation =
            """
            mutation {
                doTaakPayment(
                    paymentRequest: { 
                        identifier: "belastingzaken", 
                        taakId: "58fad5ab-dc2f-11ec-9075-f22a405ce708"
                    }
                ) {
                redirectUrl,
                }
            }
            """.trimIndent()

        val tester =
            httpGraphQlTester
                .mutate()
                .header(HttpHeaders.REFERER, "https://example.com/checkout")
                .build()
        val responseBody =
            tester
                .document(mutation)
                .execute()
                .errors()
                .verify()
                .path("doTaakPayment")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("https://payment.preprod.direct.worldline-solutions.com/hostedcheckout/PaymentMethods/Selection/e61340e579e04172a740676ffdda162e", responseBody.get("redirectUrl")?.stringValue())
    }

    fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "POST /v2/TAX/hostedcheckouts" -> {
                                TestHelper.mockResponseFromFile("/data/payment-response.json")
                            }

                            "GET /api/v2/objects/58fad5ab-dc2f-11ec-9075-f22a405ce708" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-taskv2.json")
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