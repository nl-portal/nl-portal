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
package nl.nlportal.payment.direct.api

import nl.nlportal.payment.direct.TestHelper
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.service.DirectPaymentService
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DirectPaymentControllerIT(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
    @Autowired private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
) {
    lateinit var server: MockWebServer
    private val updateRequests = AtomicInteger()

    @BeforeEach
    internal fun setUp() {
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
        val body =
            """
            {
              "apiVersion": "v1",
              "created": "2020-12-09T11:20:40.3744722+01:00",
              "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
              "merchantId": "TAX",
              "payment": {
                "paymentOutput": {
                  "amountOfMoney": {
                    "amount": 1000,
                    "currencyCode": "EUR"
                  },
                  "references": {
                    "merchantReference": "58fad5ab-dc2f-11ec-9075-f22a405ce707"
                  },
                  "paymentMethod": "card"
                },
                "status": "COMPLETED",
                "statusOutput": {
                  "isCancellable": false,
                  "statusCategory": "CREATED",
                  "statusCode": 9,
                  "isAuthorized": false,
                  "isRefundable": false
                },
                "id": "***3092546156***"
              },
              "type": "payment.created"
            }

            """.trimIndent()

        val signature =
            createSignature(
                body = body,
                secretKey =
                    directPaymentModuleConfiguration.properties.configurations["belastingzaken"]
                        ?.webhookApiSecret
                        .toString(),
            )
        val headers = HttpHeaders()
        headers.add(DirectPaymentService.HEADER_X_GCS_SIGNATURE, signature)
        headers.add(
            DirectPaymentService.HEADER_X_GCS_KEYID,
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]?.webhookApiKey,
        )
        webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.addAll(headers)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .returnResult()
            .responseBody
            .contentToString()
            .contentEquals("Request Successful processed")
    }

    @Test
    fun postSaleTestOrderIdNotUUID() {
        val body =
            """
            {
              "apiVersion": "v1",
              "created": "2020-12-09T11:20:40.3744722+01:00",
              "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
              "merchantId": "TAX",
              "payment": {
                "paymentOutput": {
                  "amountOfMoney": {
                    "amount": 1000,
                    "currencyCode": "EUR"
                  },
                  "references": {
                    "merchantReference": "58fad5ab"
                  },
                  "paymentMethod": "card"
                },
                "status": "COMPLETED",
                "statusOutput": {
                  "isCancellable": false,
                  "statusCategory": "CREATED",
                  "statusCode": 9,
                  "isAuthorized": false,
                  "isRefundable": false
                },
                "id": "***3092546156***"
              },
              "type": "payment.created"
            }

            """.trimIndent()

        val signature =
            createSignature(
                body = body,
                secretKey =
                    directPaymentModuleConfiguration.properties.configurations["belastingzaken"]
                        ?.webhookApiSecret
                        .toString(),
            )
        val headers = HttpHeaders()
        headers.add(DirectPaymentService.HEADER_X_GCS_SIGNATURE, signature)
        headers.add(
            DirectPaymentService.HEADER_X_GCS_KEYID,
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]?.webhookApiKey,
        )
        webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.addAll(headers)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .returnResult()
            .responseBody
            .contentToString()
            .contentEquals("Request Successful processed")
    }

    @Test
    fun postSaleTestInvalid() {
        val body =
            """
            {
              "apiVersion": "v1",
              "created": "2020-12-09T11:20:40.3744722+01:00",
              "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
              "merchantId": "TAX",
              "payment": {
                "paymentOutput": {
                  "amountOfMoney": {
                    "amount": 1000,
                    "currencyCode": "EUR"
                  },
                  "references": {
                    "merchantReference": "58fad5ab-dc2f-11ec-9075-f22a405ce707"
                  },
                  "paymentMethod": "card"
                },
                "status": "COMPLETED",
                "statusOutput": {
                  "isCancellable": false,
                  "statusCategory": "CREATED",
                  "statusCode": 9,
                  "isAuthorized": false,
                  "isRefundable": false
                },
                "id": "***3092546156***"
              },
              "type": "payment.created"
            }

            """.trimIndent()

        val signature =
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]
                ?.webhookApiSecret
                .toString()
        val headers = HttpHeaders()
        headers.add(DirectPaymentService.HEADER_X_GCS_SIGNATURE, signature)
        headers.add(
            DirectPaymentService.HEADER_X_GCS_KEYID,
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]?.webhookApiKey,
        )
        webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.addAll(headers)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .returnResult()
            .responseBody
            .contentToString()
            .contentEquals("Request is not valid")
    }

    @Test
    fun postSaleTestIncorrectOgoneStatus() {
        val body =
            """
            {
              "apiVersion": "v1",
              "created": "2020-12-09T11:20:40.3744722+01:00",
              "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
              "merchantId": "TAX",
              "payment": {
                "paymentOutput": {
                  "amountOfMoney": {
                    "amount": 1000,
                    "currencyCode": "EUR"
                  },
                  "references": {
                    "merchantReference": "58fad5ab-dc2f-11ec-9075-f22a405ce707"
                  },
                  "paymentMethod": "card"
                },
                "status": "COMPLETED",
                "statusOutput": {
                  "isCancellable": false,
                  "statusCategory": "CREATED",
                  "statusCode": 50,
                  "isAuthorized": false,
                  "isRefundable": false
                },
                "id": "***3092546156***"
              },
              "type": "payment.created"
            }

            """.trimIndent()

        val signature =
            createSignature(
                body = body,
                secretKey =
                    directPaymentModuleConfiguration.properties.configurations["belastingzaken"]
                        ?.webhookApiSecret
                        .toString(),
            )
        val headers = HttpHeaders()
        headers.add(DirectPaymentService.HEADER_X_GCS_SIGNATURE, signature)
        headers.add(
            DirectPaymentService.HEADER_X_GCS_KEYID,
            directPaymentModuleConfiguration.properties.configurations["belastingzaken"]?.webhookApiKey,
        )
        webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.addAll(headers)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .returnResult()
            .responseBody
            .contentToString()
            .contentEquals("200 OK \"Request has not the correct status\"")
    }

    @ParameterizedTest
    @ValueSource(ints = [91, 92, 93])
    fun postSalePendingStatusDoesNotCompleteTask(statusCode: Int) {
        assertEquals("Request has not the correct status: $statusCode", postSale(statusCode = statusCode))
        assertEquals(0, updateRequests.get())
    }

    @Test
    fun postSaleOtherMerchantDoesNotCompleteTask() {
        assertEquals("Merchant does not match the task", postSale(merchantId = "OTHER"))
        assertEquals(0, updateRequests.get())
    }

    @Test
    fun postSaleAmountOneCentOffDoesNotCompleteTask() {
        assertEquals("Amount is not the same as the amount in the task", postSale(amount = 14742))
        assertEquals(0, updateRequests.get())
    }

    @Test
    fun postSaleExactAmountCompletesTask() {
        assertEquals("Request successful processed for order $TASK_ID", postSale(amount = 14743))
        assertEquals(1, updateRequests.get())
    }

    @Test
    fun postSaleAmountAsChargedByCheckoutCompletesTask() {
        assertEquals(
            "Request successful processed for order $TASK_ID_BEDRAG_0_29",
            postSale(amount = 28, merchantReference = TASK_ID_BEDRAG_0_29),
        )
        assertEquals(1, updateRequests.get())
    }

    @Test
    fun postSaleForTaakWithProfileKeyAsPspidCompletesTask() {
        assertEquals(
            "Request successful processed for order $TASK_ID_PSPID_PROFIELNAAM",
            postSale(merchantReference = TASK_ID_PSPID_PROFIELNAAM),
        )
        assertEquals(1, updateRequests.get())
    }

    private fun postSale(
        statusCode: Int = 9,
        merchantId: String = "TAX",
        amount: Long = 14743,
        merchantReference: String = TASK_ID,
    ): String? {
        val body =
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
        val profile = directPaymentModuleConfiguration.properties.configurations["belastingzaken"]!!
        return webTestClient
            .post()
            .uri(PAYMENT_DIRECT_POSTSALE_ENDPOINT)
            .headers {
                it.add(DirectPaymentService.HEADER_X_GCS_SIGNATURE, createSignature(body, profile.webhookApiSecret))
                it.add(DirectPaymentService.HEADER_X_GCS_KEYID, profile.webhookApiKey)
            }.contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
    }

    fun setupMockObjectsApiServer() {
        updateRequests.set(0)
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    if (request.method == "PUT" || request.method == "PATCH") {
                        updateRequests.incrementAndGet()
                    }
                    val response =
                        when (request.method + " " + path) {
                            "GET /api/v2/objects/58fad5ab-dc2f-11ec-9075-f22a405ce707" -> {
                                TestHelper.mockResponseFromFile("/data/get-task.json")
                            }
                            "PUT /api/v2/objects/58fad5ab-dc2f-11ec-9075-f22a405ce707" -> {
                                TestHelper.mockResponseFromFile("/data/put-task.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_BEDRAG_0_29" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-bedrag-0-29.json")
                            }
                            "PUT /api/v2/objects/$TASK_ID_BEDRAG_0_29" -> {
                                TestHelper.mockResponseFromFile("/data/put-task.json")
                            }
                            "GET /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/get-task-pspid-profielnaam.json")
                            }
                            "PUT /api/v2/objects/$TASK_ID_PSPID_PROFIELNAAM" -> {
                                TestHelper.mockResponseFromFile("/data/put-task.json")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    return response
                }
            }
        server.dispatcher = dispatcher
    }

    companion object {
        const val PAYMENT_DIRECT_POSTSALE_ENDPOINT: String = "/api/public/payment/direct/postsale"
        const val TASK_ID: String = "58fad5ab-dc2f-11ec-9075-f22a405ce707"
        const val TASK_ID_BEDRAG_0_29: String = "8a1e5c2b-3f4d-4e6a-9b7c-1d2e3f4a5b6c"
        const val TASK_ID_PSPID_PROFIELNAAM: String = "3d5f7b9c-1e2a-4c6e-8a0b-5d7f9b1c3e5a"

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