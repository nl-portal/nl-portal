/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.payment.direct.service

import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration.DirectPaymentProfile
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration.DirectPaymentProperties
import nl.nlportal.payment.direct.domain.DirectPaymentException
import nl.nlportal.payment.direct.domain.DirectPaymentWebhookEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class DirectPaymentWebhookServiceTest {
    private val properties =
        DirectPaymentProperties(
            configurations =
                mapOf(
                    "belastingzaken" to
                        DirectPaymentProfile(
                            pspId = "TAX",
                            webhookApiKey = WEBHOOK_API_KEY,
                            webhookApiSecret = WEBHOOK_API_SECRET,
                        ),
                    "anderemerchant" to
                        DirectPaymentProfile(
                            pspId = "OTHER",
                            webhookApiKey = "7b9d1f3a5c7e9b1d3f5a7c9e1b3d5f",
                            webhookApiSecret = "9c1e3a5b-7d9f-4b1c-8e3a-5c7e9b1d3f5a",
                        ),
                ),
        )
    private val directPaymentWebhookService =
        DirectPaymentWebhookService(DirectPaymentModuleConfiguration(enabled = true, properties = properties))

    @Test
    fun `parsePostSale returns the payment for a valid signature`() {
        val body = webhookBody(statusCode = 9)

        val event = directPaymentWebhookService.parsePostSale(signedHeaders(body), body)

        assertEquals(DirectPaymentWebhookEvent(orderId = ORDER_ID, merchantId = "TAX", amount = 29, status = 9), event)
    }

    @Test
    fun `parsePostSale rejects an invalid signature`() {
        val body = webhookBody(statusCode = 9)

        assertThrows<DirectPaymentException.InvalidSignature> {
            directPaymentWebhookService.parsePostSale(signedHeaders(webhookBody(statusCode = 1)), body)
        }
    }

    @Test
    fun `parsePostSale rejects a payment that is not final`() {
        val body = webhookBody(statusCode = 91)

        val exception =
            assertThrows<DirectPaymentException.PaymentNotFinal> {
                directPaymentWebhookService.parsePostSale(signedHeaders(body), body)
            }
        assertEquals(91, exception.status)
    }

    @Test
    fun `verifyPayment accepts the exact amount for the merchant by key or pspId`() {
        assertDoesNotThrow { directPaymentWebhookService.verifyPayment(event(amount = 29), "TAX", 0.29) }
        assertDoesNotThrow { directPaymentWebhookService.verifyPayment(event(amount = 29), "belastingzaken", 0.29) }
    }

    @Test
    fun `verifyPayment rejects another merchant`() {
        assertThrows<DirectPaymentException.MerchantMismatch> {
            directPaymentWebhookService.verifyPayment(event(amount = 29), "anderemerchant", 0.29)
        }
        assertThrows<DirectPaymentException.MerchantMismatch> {
            directPaymentWebhookService.verifyPayment(event(amount = 29), "onbekend", 0.29)
        }
    }

    @Test
    fun `verifyPayment rejects another amount`() {
        assertThrows<DirectPaymentException.AmountMismatch> {
            directPaymentWebhookService.verifyPayment(event(amount = 28), "TAX", 0.29)
        }
        assertThrows<DirectPaymentException.AmountMismatch> {
            directPaymentWebhookService.verifyPayment(event(amount = 1001), "TAX", 10.005)
        }
    }

    @Test
    fun `verifyPayment rejects legacy rounded amounts by default`() {
        assertThrows<DirectPaymentException.AmountMismatch> {
            directPaymentWebhookService.verifyPayment(event(amount = 28), "TAX", 0.29)
        }
    }

    @Test
    fun `verifyPayment accepts legacy rounded amounts when enabled`() {
        val lenientService =
            DirectPaymentWebhookService(
                DirectPaymentModuleConfiguration(enabled = true, properties = properties.copy(acceptLegacyAmountRounding = true)),
            )

        assertDoesNotThrow { lenientService.verifyPayment(event(amount = 28), "TAX", 0.29) }
        assertDoesNotThrow { lenientService.verifyPayment(event(amount = 29), "TAX", 0.29) }
        assertThrows<DirectPaymentException.AmountMismatch> { lenientService.verifyPayment(event(amount = 27), "TAX", 0.29) }
    }

    private fun event(amount: Long) = DirectPaymentWebhookEvent(orderId = ORDER_ID, merchantId = "TAX", amount = amount, status = 9)

    private fun signedHeaders(body: String): HttpHeaders {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(WEBHOOK_API_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val headers = HttpHeaders()
        headers.add(DirectPaymentWebhookService.HEADER_X_GCS_SIGNATURE, Base64.getEncoder().encodeToString(hmac.doFinal(body.toByteArray(Charsets.UTF_8))))
        headers.add(DirectPaymentWebhookService.HEADER_X_GCS_KEYID, WEBHOOK_API_KEY)
        return headers
    }

    private fun webhookBody(statusCode: Int): String =
        """
        {
          "apiVersion": "v1",
          "created": "2020-12-09T11:20:40.3744722+01:00",
          "id": "34b8a607-1fce-4003-b3ae-a4d29e92b232",
          "merchantId": "TAX",
          "payment": {
            "paymentOutput": {
              "amountOfMoney": {
                "amount": 29,
                "currencyCode": "EUR"
              },
              "references": {
                "merchantReference": "$ORDER_ID"
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

    companion object {
        const val ORDER_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce709"
        const val WEBHOOK_API_KEY = "0f3c5a7e9b1d4f6a8c0e2b4d6f8a1c"
        const val WEBHOOK_API_SECRET = "3e5a7c9e-1b3d-4f5a-8c7e-9a1b3c5d7e9f"
    }
}