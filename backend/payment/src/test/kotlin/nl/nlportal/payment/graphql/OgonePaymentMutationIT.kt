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
package nl.nlportal.payment.graphql

import com.fasterxml.jackson.databind.JsonNode
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.payment.TestHelper
import nl.nlportal.payment.autoconfiguration.OgonePaymentConfig
import nl.nlportal.payment.domain.OgonePayment
import nl.nlportal.payment.domain.PaymentField
import nl.nlportal.payment.service.OgonePaymentService
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(PER_CLASS)
internal class OgonePaymentMutationIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val paymentConfig: OgonePaymentConfig,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
) {
    lateinit var server: MockWebServer

    @BeforeAll
    internal fun setUp() {
        server = MockWebServer()
        setupMockObjectsApiServer()
        server.start()
        objectsApiClientConfig.properties.url = server.url("/").toUri()
    }

    @AfterAll
    internal fun tearDown() {
        server.shutdown()
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentWithIdentifier() {
        val fields = generateOgonePayment(pspId = "belastingzaken")

        assertEquals("http://localhost:3000", fields[OgonePayment.PAYMENT_PROPERTY_ACCEPT_URL])
        assertEquals("TAX", fields[OgonePayment.PAYMENT_PROPERTY_PSPID])
        assertEquals(expectedShaSign(fields), fields[OgonePayment.PAYMENT_PROPERTY_SHASIGN])
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentWithPspId() {
        val fields = generateOgonePayment(pspId = "TAX")

        assertEquals("TAX", fields[OgonePayment.PAYMENT_PROPERTY_PSPID])
        assertEquals(expectedShaSign(fields), fields[OgonePayment.PAYMENT_PROPERTY_SHASIGN])
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentTakesValuesFromTaak() {
        val fields = generateOgonePayment(amount = "0.01", reference = "x", title = "Andere titel")

        assertEquals("14743", fields[OgonePayment.PAYMENT_PROPERTY_AMOUNT])
        assertEquals("abcdef1234", fields[OgonePayment.PAYMENT_PROPERTY_COM])
        assertEquals("Een betaal taak", fields[OgonePayment.PAYMENT_PROPERTY_TITLE])
        assertEquals(TASK_ID, fields[OgonePayment.PAYMENT_PROPERTY_ORDER_ID])
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentForTaakWithProfileKeyAsPspid() {
        val fields = generateOgonePayment(orderId = TASK_ID_PSPID_PROFIELNAAM)

        assertEquals("TAX", fields[OgonePayment.PAYMENT_PROPERTY_PSPID])
        assertEquals(TASK_ID_PSPID_PROFIELNAAM, fields[OgonePayment.PAYMENT_PROPERTY_ORDER_ID])
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentForTaakOfOtherUser() {
        assertRejected(ogonePaymentMutation(orderId = TASK_ID_OTHER_USER), "Access denied to this taak")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentForAfgerondTaak() {
        assertRejected(ogonePaymentMutation(orderId = TASK_ID_AFGEROND), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentWithOrderIdNotUUID() {
        assertRejected(ogonePaymentMutation(orderId = "123456"), "Taak payment not found")
    }

    @Test
    @WithBurgerUser(TASK_BSN)
    fun generateOgonePaymentWithProfileOfOtherMerchant() {
        assertRejected(ogonePaymentMutation(pspId = "OTHER"), "Payment profile does not match the taak")
    }

    private fun ogonePaymentMutation(
        pspId: String = "TAX",
        orderId: String = TASK_ID,
        amount: String = "100.25",
        reference: String = "12345",
        title: String = "Gemeente belastingen 2024",
    ): String =
        """
        mutation {
            generateOgonePayment(
                paymentRequest: { pspId: "$pspId", amount: $amount, orderId: "$orderId", reference: "$reference", title: "$title" }
            ) {
            formAction,
            formFields{
                name,
                value
            }
            }
        }
        """.trimIndent()

    private fun generateOgonePayment(
        pspId: String = "TAX",
        orderId: String = TASK_ID,
        amount: String = "100.25",
        reference: String = "12345",
        title: String = "Gemeente belastingen 2024",
    ): Map<String, String> =
        httpGraphQlTester
            .document(ogonePaymentMutation(pspId = pspId, orderId = orderId, amount = amount, reference = reference, title = title))
            .execute()
            .errors()
            .verify()
            .path("generateOgonePayment.formFields")
            .entity(JsonNode::class.java)
            .get()
            .associate { it.get("name").textValue() to it.get("value").textValue() }

    private fun expectedShaSign(fields: Map<String, String>): String {
        val profile = paymentConfig.properties.getPaymentProfile("belastingzaken")!!
        return OgonePaymentService.hashParameters(
            fields.filterKeys { it != OgonePayment.PAYMENT_PROPERTY_SHASIGN }.map { PaymentField(it.key, it.value) },
            profile.shaInKey,
            profile.shaVersion,
        )
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
    }

    fun setupMockObjectsApiServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "GET /api/v2/objects/$TASK_ID" -> {
                                TestHelper.mockResponseFromFile("/data/get-task.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_OTHER_USER" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-other-user.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_AFGEROND" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-afgerond.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    return response
                }
            }
        server.dispatcher = dispatcher
    }

    companion object {
        const val TASK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce707"
        const val TASK_ID_OTHER_USER = "1c9e7a5b-2d4f-4a6c-8e0b-3f5a7c9e1b2d"
        const val TASK_ID_AFGEROND = "4b6d8f0a-1c3e-4a5b-9d7f-2e4a6c8e0b1d"
        const val TASK_ID_PSPID_PROFIELNAAM = "3d5f7b9c-1e2a-4c6e-8a0b-5d7f9b1c3e5a"
        const val TASK_BSN = "569312863"
    }
}