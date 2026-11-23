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

import com.onlinepayments.communication.RequestHeader
import com.onlinepayments.json.DefaultMarshaller
import com.onlinepayments.webhooks.InMemorySecretKeyStore
import com.onlinepayments.webhooks.SignatureValidationException
import com.onlinepayments.webhooks.WebhooksHelper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.util.Mapper
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.constants.DirectPaymentState
import nl.nlportal.payment.direct.domain.DirectPaymentException
import nl.nlportal.payment.direct.domain.DirectPaymentWebhookEvent
import nl.nlportal.payment.direct.domain.DirectPaymentWebhookRequest
import org.springframework.http.HttpHeaders

class DirectPaymentWebhookService(
    private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
) {
    private val webhookHelper = WebhooksHelper(DefaultMarshaller.INSTANCE, InMemorySecretKeyStore.INSTANCE)

    init {
        directPaymentModuleConfiguration.properties.configurations.forEach {
            InMemorySecretKeyStore.INSTANCE.storeSecretKey(it.value.webhookApiKey, it.value.webhookApiSecret)
        }
    }

    /**
     * Parse a postsale webhook call from the payment provider.
     * @param httpHeaders: headers of the request, with the signature headers
     * @param jsonBody: the raw body of the request
     * @return the successful payment of the webhook call
     * @throws DirectPaymentException.InvalidSignature when the signature is not valid
     * @throws DirectPaymentException.PaymentNotFinal when the payment status is not successful
     */
    fun parsePostSale(
        httpHeaders: HttpHeaders,
        jsonBody: String,
    ): DirectPaymentWebhookEvent {
        val requestHeaders =
            listOf(
                RequestHeader(HEADER_X_GCS_SIGNATURE, httpHeaders[HEADER_X_GCS_SIGNATURE]?.get(0)),
                RequestHeader(HEADER_X_GCS_KEYID, httpHeaders[HEADER_X_GCS_KEYID]?.get(0)),
            )
        try {
            webhookHelper.unmarshal(jsonBody, requestHeaders)
        } catch (ex: SignatureValidationException) {
            logger.debug { "Direct payment webhook signature is not valid" }
            throw DirectPaymentException.InvalidSignature()
        }

        val request = Mapper.get().readValue(jsonBody, DirectPaymentWebhookRequest::class.java)
        val event =
            DirectPaymentWebhookEvent(
                orderId = request.payment.paymentOutput.references.merchantReference,
                merchantId = request.merchantId,
                amount = request.payment.paymentOutput.amountOfMoney.amount,
                status = request.payment.statusOutput.statusCode,
            )
        if (event.status != DirectPaymentState.SUCCESS.status) {
            logger.debug { "Direct payment webhook for order ${event.orderId} has status ${event.status}" }
            throw DirectPaymentException.PaymentNotFinal(event.status)
        }
        return event
    }

    /**
     * Verifies that a successful payment was made to the merchant for the expected amount. With acceptLegacyAmountRounding
     * the amount converted to cents the 3.x way is accepted as well.
     * @param event: the successful payment of the webhook call
     * @param merchant: payment profile key or pspId of the merchant the order belongs to
     * @param amount: the expected amount in euros
     * @throws DirectPaymentException.MerchantMismatch when the payment was made to another merchant
     * @throws DirectPaymentException.AmountMismatch when the paid amount differs from the expected amount
     */
    fun verifyPayment(
        event: DirectPaymentWebhookEvent,
        merchant: String,
        amount: Double,
    ) {
        val properties = directPaymentModuleConfiguration.properties
        val merchantProfile = properties.getPaymentProfile(merchant) ?: properties.getPaymentProfileByPspPid(merchant)
        if (event.merchantId != merchantProfile?.pspId) {
            logger.debug { "Direct payment webhook for order ${event.orderId} has another merchant" }
            throw DirectPaymentException.MerchantMismatch()
        }
        val accepted =
            runCatching { DirectPaymentService.toCents(amount) }.getOrNull() == event.amount ||
                (properties.acceptLegacyAmountRounding && (amount * 100).toLong() == event.amount)
        if (!accepted) {
            logger.debug { "Direct payment webhook for order ${event.orderId} has another amount" }
            throw DirectPaymentException.AmountMismatch()
        }
    }

    companion object {
        private val logger: KLogger = KotlinLogging.logger {}
        const val HEADER_X_GCS_SIGNATURE: String = "X-GCS-Signature"
        const val HEADER_X_GCS_KEYID: String = "X-GCS-KeyId"
    }
}