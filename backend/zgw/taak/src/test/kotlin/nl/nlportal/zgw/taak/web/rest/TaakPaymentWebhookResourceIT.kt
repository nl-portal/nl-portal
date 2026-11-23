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
package nl.nlportal.zgw.taak.web.rest

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.service.DirectPaymentWebhookService
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import nl.nlportal.zgw.taak.TestHelper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TaakPaymentWebhookResourceIT(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
) {
    lateinit var server: MockWebServer
    private val objectUpdates = AtomicInteger()

    @BeforeEach
    internal fun setUp() {
        objectUpdates.set(0)
        server = MockWebServer()
        setupMockObjectsApiServer()
        server.start()
        objectsApiClientConfig.properties.url = server.url("/").toUri()
    }

    @AfterEach
    internal fun tearDown() {
        server.shutdown()
    }

    @Test
    fun postSaleTest() {
        val response = postSale(webhookBody(amount = 14743))

        assertEquals("Request successful processed for order $TASK_ID", response)
        assertEquals(1, objectUpdates.get())
    }

    @Test
    fun postSaleTestOrderIdNotUUID() {
        val response = postSale(webhookBody(merchantReference = "58fad5ab", amount = 14743))

        assertEquals("Request successful processed for order 58fad5ab", response)
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestInvalid() {
        val response = postSale(webhookBody(amount = 14743), signature = webhookApiSecret())

        assertEquals("Request is not valid", response)
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestIncorrectOgoneStatus() {
        listOf(50, 91, 92, 93).forEach { statusCode ->
            val response = postSale(webhookBody(amount = 14743, statusCode = statusCode))

            assertEquals("Request has not the correct status: $statusCode", response)
        }
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestOtherMerchant() {
        val response = postSale(webhookBody(merchantId = "OTHER", amount = 14743))

        assertEquals("Merchant does not match the task", response)
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestAmountOneCentOff() {
        val response = postSale(webhookBody(amount = 14742))

        assertEquals("Amount is not the same as the amount in the task", response)
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestExactCents() {
        val response = postSale(webhookBody(merchantReference = TASK_ID_BEDRAG_0_29, amount = 29))

        assertEquals("Request successful processed for order $TASK_ID_BEDRAG_0_29", response)
        assertEquals(1, objectUpdates.get())
    }

    @Test
    fun postSaleTestTruncatedCents() {
        val response = postSale(webhookBody(merchantReference = TASK_ID_BEDRAG_0_29, amount = 28))

        assertEquals("Amount is not the same as the amount in the task", response)
        assertEquals(0, objectUpdates.get())
    }

    @Test
    fun postSaleTestPspidIsProfileKey() {
        val response = postSale(webhookBody(merchantReference = TASK_ID_PSPID_PROFIELNAAM, amount = 14743))

        assertEquals("Request successful processed for order $TASK_ID_PSPID_PROFIELNAAM", response)
        assertEquals(1, objectUpdates.get())
    }

    private fun postSale(
        body: String,
        signature: String = createSignature(body = body, secretKey = webhookApiSecret()),
    ): String? {
        val headers = HttpHeaders()
        headers.add(DirectPaymentWebhookService.HEADER_X_GCS_SIGNATURE, signature)
        headers.add(
            DirectPaymentWebhookService.HEADER_X_GCS_KEYID,
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]?.webhookApiKey,
        )
        return webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.addAll(headers)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
    }

    private fun webhookApiSecret(): String =
        directPaymentModuleConfiguration.properties.configurations["belastingzaken"]
            ?.webhookApiSecret
            .toString()

    private fun webhookBody(
        merchantReference: String = TASK_ID,
        merchantId: String = "TAX",
        amount: Long,
        statusCode: Int = 9,
    ): String =
        """
        {
          "apiVersion": "v1",
          "created": "2020-12-09T11:20:40.3744722+01:00",
          "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
          "merchantId": "$merchantId",
          "payment": {
            "paymentOutput": {
              "amountOfMoney": {
                "amount": $amount,
                "currencyCode": "EUR"
              },
              "references": {
                "merchantReference": "$merchantReference"
              },
              "paymentMethod": "card"
            },
            "status": "COMPLETED",
            "statusOutput": {
              "isCancellable": false,
              "statusCategory": "CREATED",
              "statusCode": $statusCode,
              "isAuthorized": false,
              "isRefundable": false
            },
            "id": "***3092546156***"
          },
          "type": "payment.created"
        }

        """.trimIndent()

    fun setupMockObjectsApiServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.method == "PUT" || request.method == "PATCH") {
                        objectUpdates.incrementAndGet()
                    }
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "GET /api/v2/objects/$TASK_ID" -> {
                                TestHelper.mockResponseFromFile("/data/get-task.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_BEDRAG_0_29" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-bedrag-0-29.json")
                            }

                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }

                            "PUT /api/v2/objects/$TASK_ID",
                            "PUT /api/v2/objects/$TASK_ID_BEDRAG_0_29",
                            "PUT /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM",
                            -> {
                                TestHelper.mockResponseFromFile("/data/put-task.json")
                            }

                            else -> {
                                MockResponse().setResponseCode(404)
                            }
                        }
                    return response
                }
            }
        server.dispatcher = dispatcher
    }

    companion object {
        const val PAYMENT_DIRECT_POSTSALE_ENDPOINT: String = "/api/public/payment/direct/postsale"
        const val TASK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce707"
        const val TASK_ID_BEDRAG_0_29 = "58fad5ab-dc2f-11ec-9075-f22a405ce709"
        const val TASK_ID_PSPID_PROFIELNAAM = "58fad5ab-dc2f-11ec-9075-f22a405ce710"

        fun createSignature(
            body: String,
            secretKey: String,
        ): String {
            val hmac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(secretKey.toByteArray(charset = Charset.forName("UTF-8")), "HmacSHA256")
            hmac.init(key)

            val unencodedResult = hmac.doFinal(body.toByteArray(charset = Charset.forName("UTF-8")))
            return Base64.encodeBase64String(unencodedResult)
        }
    }
}