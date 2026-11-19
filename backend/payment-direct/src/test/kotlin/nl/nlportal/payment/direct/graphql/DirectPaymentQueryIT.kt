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
package nl.nlportal.payment.direct.graphql

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.payment.direct.TestHelper
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class DirectPaymentQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
) {
    companion object {
        private val logger: KLogger = KotlinLogging.logger {}
        const val TASK_BSN = "569312863"

        @JvmStatic
        var server: MockWebServer? = null

        @JvmStatic
        var url: String = ""

        @JvmStatic
        @DynamicPropertySource
        fun properties(propsRegistry: DynamicPropertyRegistry) {
            propsRegistry.add("nl-portal.config.payment.direct.properties.url") { url }
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
        objectsApiClientConfig.properties.url = server!!.url("/").toUri()
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatus() {
        val responseBody =
            httpGraphQlTester
                .document(statusQuery(hostedCheckoutId = "4373041363"))
                .execute()
                .errors()
                .verify()
                .path("getDirectPaymentStatus")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("SUCCESSFUL", responseBody.get("status")?.textValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusForTaakWithProfileKeyAsPspid() {
        val responseBody =
            httpGraphQlTester
                .document(statusQuery(hostedCheckoutId = "5555555555"))
                .execute()
                .errors()
                .verify()
                .path("getDirectPaymentStatus")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("SUCCESSFUL", responseBody.get("status")?.textValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusForTaakOfOtherUser() {
        assertRejected(statusQuery(hostedCheckoutId = "1111111111"), "Access denied to this taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusWithReferenceNotUUID() {
        assertRejected(statusQuery(hostedCheckoutId = "2222222222"), "Payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusWithoutPayment() {
        assertRejected(statusQuery(hostedCheckoutId = "3333333333"), "Payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusWithProfileOfOtherMerchant() {
        assertRejected(statusQuery(identifier = "anderemerchant", hostedCheckoutId = "4373041363"), "Payment not found")
    }

    private fun statusQuery(
        identifier: String = "belastingzaken",
        hostedCheckoutId: String,
    ): String =
        """
        query {
            getDirectPaymentStatus(
                identifier: "$identifier",
                hostedCheckoutId: "$hostedCheckoutId"
            ) {
            status,
            }
        }
        """.trimIndent()

    private fun assertRejected(
        query: String,
        reason: String,
    ) {
        httpGraphQlTester
            .document(query)
            .execute()
            .errors()
            .satisfy { errors -> assertTrue(errors.any { it.message?.contains(reason) == true }, errors.toString()) }
    }

    fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "GET /v2/TAX/hostedcheckouts/4373041363" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response.json")
                            }
                            "GET /v2/OTHER/hostedcheckouts/4373041363" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response.json")
                            }
                            "GET /v2/TAX/hostedcheckouts/1111111111" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response-other-user.json")
                            }
                            "GET /v2/TAX/hostedcheckouts/2222222222" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response-reference-not-uuid.json")
                            }
                            "GET /v2/TAX/hostedcheckouts/3333333333" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response-without-payment.json")
                            }
                            "GET /v2/TAX/hostedcheckouts/5555555555" -> {
                                TestHelper.mockResponseFromFile("/data/payment-status-response-pspid-profielnaam.json")
                            }
                            "GET /api/v2/objects/3d5f7b9c-1e2a-4c6e-8a0b-5d7f9b1c3e5a" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }
                            "GET /api/v2/objects/58fad5ab-dc2f-11ec-9075-f22a405ce707" -> {
                                TestHelper.mockResponseFromFile("/data/get-task.json")
                            }
                            "GET /api/v2/objects/1c9e7a5b-2d4f-4a6c-8e0b-3f5a7c9e1b2d" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-other-user.json")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}