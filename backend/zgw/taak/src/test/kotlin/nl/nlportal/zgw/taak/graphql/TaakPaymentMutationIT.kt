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
import java.util.concurrent.TimeUnit
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
import org.junit.jupiter.api.Assertions.assertNull
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
import tools.jackson.databind.json.JsonMapper

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
        const val TASK_BSN = "569312863"
        const val TASK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce708"
        const val TASK_ID_PSPID_PROFIELNAAM = "58fad5ab-dc2f-11ec-9075-f22a405ce710"
        const val TASK_ID_OTHER_USER = "58fad5ab-dc2f-11ec-9075-f22a405ce711"
        const val TASK_ID_AFGEROND = "58fad5ab-dc2f-11ec-9075-f22a405ce712"
        const val TASK_ID_ZONDER_BETALING = "58fad5ab-dc2f-11ec-9075-f22a405ce713"

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
        while (server!!.takeRequest(0, TimeUnit.SECONDS) != null) {
        }
        setupMockServer()
        url = server?.url("/").toString()
        directPaymentModuleConfiguration.properties.url = url
        objectsApiClientConfig.properties.url = URI(url)
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithIdentifier() {
        val responseBody =
            httpGraphQlTester
                .document(doTaakPaymentMutation())
                .execute()
                .errors()
                .verify()
                .path("doTaakPayment")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("https://payment.preprod.direct.worldline-solutions.com/hostedcheckout/PaymentMethods/Selection/e61340e579e04172a740676ffdda162e", responseBody.get("redirectUrl")?.stringValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentTakesAmountAndReferenceFromTaak() {
        doTaakPayment(doTaakPaymentMutation())

        val order = sentCheckout()!!.path("order")
        assertEquals(10024, order.path("amountOfMoney").path("amount").longValue())
        assertEquals("58fad5ab-dc2f-11ec-9075-f22a405ce708", order.path("references").path("descriptor").stringValue())
        assertEquals(TASK_ID, order.path("references").path("merchantReference").stringValue())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakWithProfileKeyAsPspid() {
        doTaakPayment(doTaakPaymentMutation(identifier = "TAX", taakId = TASK_ID_PSPID_PROFIELNAAM))

        assertEquals(
            TASK_ID_PSPID_PROFIELNAAM,
            sentCheckout()!!
                .path("order")
                .path("references")
                .path("merchantReference")
                .stringValue(),
        )
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakOfOtherUser() {
        assertRejected(doTaakPaymentMutation(taakId = TASK_ID_OTHER_USER), "Access denied to this taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForAfgerondTaak() {
        assertRejected(doTaakPaymentMutation(taakId = TASK_ID_AFGEROND), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentForTaakWithoutBetaling() {
        assertRejected(doTaakPaymentMutation(taakId = TASK_ID_ZONDER_BETALING), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithProfileOfOtherMerchant() {
        assertRejected(doTaakPaymentMutation(identifier = "anderemerchant"), "Payment profile does not match the taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithSameOriginReturnUrl() {
        doTaakPayment(doTaakPaymentMutation(returnUrl = "http://localhost:3000/zaken/zaak/1?type=ogone"))

        assertEquals("http://localhost:3000/zaken/zaak/1?type=ogone", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithForeignReturnUrl() {
        doTaakPayment(doTaakPaymentMutation(returnUrl = "https://evil.example/"))

        assertEquals("http://localhost:3000", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithProtocolRelativeReturnUrl() {
        doTaakPayment(doTaakPaymentMutation(returnUrl = "//evil.example/"))

        assertEquals("http://localhost:3000", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithoutReturnUrl() {
        doTaakPayment(doTaakPaymentMutation())

        assertEquals("http://localhost:3000", sentReturnUrl())
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun doDirectPaymentWithOnlyRefererHeader() {
        httpGraphQlTester
            .mutate()
            .header("Referer", "https://evil.example/checkout")
            .build()
            .document(doTaakPaymentMutation())
            .execute()
            .errors()
            .verify()

        assertEquals("http://localhost:3000", sentReturnUrl())
    }

    private fun doTaakPaymentMutation(
        identifier: String = "belastingzaken",
        taakId: String = TASK_ID,
        returnUrl: String? = null,
    ): String =
        """
        mutation {
            doTaakPayment(
                paymentRequest: {
                    identifier: "$identifier",
                    taakId: "$taakId",
                    ${returnUrl?.let { "returnUrl: \"$it\"," } ?: ""}
                }
            ) {
            redirectUrl,
            }
        }
        """.trimIndent()

    private fun doTaakPayment(mutation: String) {
        httpGraphQlTester
            .document(mutation)
            .execute()
            .errors()
            .verify()
    }

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

    private fun sentReturnUrl(): String? =
        sentCheckout()
            ?.path("hostedCheckoutSpecificInput")
            ?.path("returnUrl")
            ?.stringValue()

    private fun sentCheckout(): JsonNode? {
        while (true) {
            val request = server!!.takeRequest(1, TimeUnit.SECONDS) ?: return null
            if (request.method == "POST" && request.path?.endsWith("/hostedcheckouts") == true) {
                return JsonMapper.builder().build().readTree(request.body.readUtf8())
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
                            "POST /v2/TAX/hostedcheckouts",
                            "POST /v2/OTHER/hostedcheckouts",
                            -> {
                                TestHelper.mockResponseFromFile("/data/payment-response.json")
                            }

                            "GET /api/v2/objects/$TASK_ID" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-taskv2.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_OTHER_USER" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-task-other-user.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_AFGEROND" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-task-afgerond.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_ZONDER_BETALING" -> {
                                TestHelper.mockResponseFromFile("/data/get-betaal-task-zonder-betaling.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
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