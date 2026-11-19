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
import java.util.concurrent.TimeUnit
import nl.nlportal.core.util.Mapper
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
import org.junit.jupiter.api.Assertions.assertNull
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
internal class DirectPaymentMutationIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
) {
    companion object {
        const val TASK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce707"
        const val TASK_ID_OTHER_USER = "1c9e7a5b-2d4f-4a6c-8e0b-3f5a7c9e1b2d"
        const val TASK_ID_AFGEROND = "4b6d8f0a-1c3e-4a5b-9d7f-2e4a6c8e0b1d"
        const val TASK_ID_ZONDER_BETALING = "7e9a1c3d-5f7b-4d2e-8a0c-6b8d0f2a4c6e"
        const val TASK_ID_PSPID_PROFIELNAAM = "3d5f7b9c-1e2a-4c6e-8a0b-5d7f9b1c3e5a"
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
        while (server!!.takeRequest(0, TimeUnit.SECONDS) != null) {
        }
        setupMockServer()
        url = server?.url("/").toString()
        directPaymentModuleConfiguration.properties.url = url
        objectsApiClientConfig.properties.url = server!!.url("/").toUri()
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithIdentifier() {
        val responseBody =
            httpGraphQlTester
                .document(doDirectPaymentMutation())
                .execute()
                .errors()
                .verify()
                .path("doDirectPayment")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("https://payment.preprod.direct.worldline-solutions.com/hostedcheckout/PaymentMethods/Selection/e61340e579e04172a740676ffdda162e", responseBody.get("redirectUrl")?.textValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentTakesAmountAndReferenceFromTaak() {
        httpGraphQlTester
            .document(doDirectPaymentMutation(amount = "0.01", reference = "x"))
            .execute()
            .errors()
            .verify()

        val order = sentCheckout()!!.path("order")
        assertEquals(14743, order.path("amountOfMoney").path("amount").longValue())
        assertEquals("abcdef1234", order.path("references").path("descriptor").textValue())
        assertEquals(TASK_ID, order.path("references").path("merchantReference").textValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakWithProfileKeyAsPspid() {
        httpGraphQlTester
            .document(doDirectPaymentMutation(orderId = TASK_ID_PSPID_PROFIELNAAM))
            .execute()
            .errors()
            .verify()

        assertEquals(
            TASK_ID_PSPID_PROFIELNAAM,
            sentCheckout()!!
                .path("order")
                .path("references")
                .path("merchantReference")
                .textValue(),
        )
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakOfOtherUser() {
        assertRejected(doDirectPaymentMutation(orderId = TASK_ID_OTHER_USER), "Access denied to this taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForAfgerondTaak() {
        assertRejected(doDirectPaymentMutation(orderId = TASK_ID_AFGEROND), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakWithoutBetaling() {
        assertRejected(doDirectPaymentMutation(orderId = TASK_ID_ZONDER_BETALING), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithOrderIdNotUUID() {
        assertRejected(doDirectPaymentMutation(orderId = "123456"), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithProfileOfOtherMerchant() {
        assertRejected(doDirectPaymentMutation(identifier = "anderemerchant"), "Payment profile does not match the taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithSameOriginReturnUrl() {
        doDirectPaymentWithReturnUrl("http://localhost:3000/taken/1")

        assertEquals("http://localhost:3000/taken/1", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithForeignReturnUrl() {
        doDirectPaymentWithReturnUrl("https://evil.example/")

        assertEquals("http://localhost:3000", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithNotAbsoluteProfileReturnUrl() {
        doDirectPaymentWithReturnUrl("https://evil.example/", "zonderorigin")

        assertEquals("localhost:3000", sentReturnUrl())
    }

    private fun doDirectPaymentMutation(
        identifier: String = "belastingzaken",
        orderId: String = TASK_ID,
        amount: String = "100.25",
        reference: String = "12345",
        returnUrl: String? = null,
    ): String =
        """
        mutation {
            doDirectPayment(
                paymentRequest: {
                    identifier: "$identifier",
                    amount: $amount,
                    orderId: "$orderId",
                    reference: "$reference",
                    ${returnUrl?.let { "returnUrl: \"$it\"," } ?: ""}
                }
            ) {
            redirectUrl,
            }
        }
        """.trimIndent()

    private fun assertRejected(
        mutation: String,
        reason: String,
    ) {
        httpGraphQlTester
            .document(mutation)
            .execute()
            .errors()
            .satisfy { errors -> assertTrue(errors.any { it.message?.contains(reason) == true }, errors.toString()) }

        assertNull(sentCheckout())
    }

    private fun doDirectPaymentWithReturnUrl(
        returnUrl: String,
        identifier: String = "belastingzaken",
    ) {
        httpGraphQlTester
            .document(doDirectPaymentMutation(identifier = identifier, returnUrl = returnUrl))
            .execute()
            .errors()
            .verify()
    }

    private fun sentReturnUrl(): String? =
        sentCheckout()
            ?.path("hostedCheckoutSpecificInput")
            ?.path("returnUrl")
            ?.textValue()

    private fun sentCheckout(): JsonNode? {
        while (true) {
            val request = server!!.takeRequest(1, TimeUnit.SECONDS) ?: return null
            if (request.method == "POST" && request.path?.endsWith("/hostedcheckouts") == true) {
                return Mapper.get().readTree(request.body.readUtf8())
            }
        }
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
                            "POST /v2/OTHER/hostedcheckouts" -> {
                                TestHelper.mockResponseFromFile("/data/payment-response.json")
                            }
                            "GET /api/v2/objects/$TASK_ID" -> {
                                TestHelper.mockResponseFromFile("/data/get-task.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_OTHER_USER" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-other-user.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_AFGEROND" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-afgerond.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_ZONDER_BETALING" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-zonder-betaling.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}