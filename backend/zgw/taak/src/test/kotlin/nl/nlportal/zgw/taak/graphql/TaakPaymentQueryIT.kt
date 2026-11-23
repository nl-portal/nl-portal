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

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.JsonNode

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class TaakPaymentQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
) {
    companion object {
        private val logger: KLogger = KotlinLogging.logger {}
        const val TASK_BSN = "569312863"
        const val TASK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce708"
        const val TASK_ID_PSPID_PROFIELNAAM = "58fad5ab-dc2f-11ec-9075-f22a405ce710"
        const val TASK_ID_OTHER_USER = "58fad5ab-dc2f-11ec-9075-f22a405ce711"
        const val TASK_ID_OTHER_MERCHANT = "58fad5ab-dc2f-11ec-9075-f22a405ce714"
        const val CHECKOUT_WITHOUT_PAYMENT = "4373041363"

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
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatus() {
        assertEquals("SUCCESSFUL", getTaakPaymentStatus(TASK_ID))
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusForTaakWithProfileKeyAsPspid() {
        assertEquals("SUCCESSFUL", getTaakPaymentStatus(TASK_ID_PSPID_PROFIELNAAM))
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusForTaakOfOtherUser() {
        assertRejected(TASK_ID_OTHER_USER, "Access denied to this taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusWithMerchantReferenceNotUUID() {
        assertRejected("123456")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusForTaakOfOtherMerchant() {
        assertRejected(TASK_ID_OTHER_MERCHANT)
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun getDirectPaymentStatusWithoutPayment() {
        assertRejected(CHECKOUT_WITHOUT_PAYMENT)
    }

    private fun statusQuery(hostedCheckoutId: String): String =
        """
        query {
            getTaakPaymentStatus(
                identifier: "belastingzaken",
                hostedCheckoutId: "$hostedCheckoutId"
            ) {
            status,
            }
        }
        """.trimIndent()

    private fun getTaakPaymentStatus(hostedCheckoutId: String): String? =
        httpGraphQlTester
            .document(statusQuery(hostedCheckoutId))
            .execute()
            .errors()
            .verify()
            .path("getTaakPaymentStatus")
            .entity(JsonNode::class.java)
            .get()
            .get("status")
            ?.stringValue()

    private fun assertRejected(
        hostedCheckoutId: String,
        reason: String = "Payment not found",
    ) {
        httpGraphQlTester
            .document(statusQuery(hostedCheckoutId))
            .execute()
            .errors()
            .satisfy { errors -> assertTrue(errors.any { it.message?.contains(reason) == true }, errors.toString()) }
    }

    private fun statusResponse(merchantReference: String): MockResponse =
        TestHelper
            .mockResponseFromFile("/data/payment-status-response.json")
            .setBody(
                javaClass
                    .getResource("/data/payment-status-response.json")!!
                    .readText()
                    .replace(TASK_ID, merchantReference),
            )

    fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "GET /v2/TAX/hostedcheckouts/$CHECKOUT_WITHOUT_PAYMENT" -> {
                                TestHelper
                                    .mockResponseFromFile("/data/payment-status-response.json")
                                    .setBody("""{"createdPaymentOutput": {"paymentStatusCategory": "SUCCESSFUL"}, "status": "PAYMENT_CREATED"}""")
                            }

                            "GET /api/v2/objects/$TASK_ID" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-taskv2.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_OTHER_USER" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-task-other-user.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_OTHER_MERCHANT" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-task-other-merchant.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }

                            else -> {
                                if (request.method == "GET" && path?.startsWith("/v2/TAX/hostedcheckouts/") == true) {
                                    statusResponse(path.substringAfterLast('/'))
                                } else {
                                    MockResponse().setResponseCode(404)
                                }
                            }
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}