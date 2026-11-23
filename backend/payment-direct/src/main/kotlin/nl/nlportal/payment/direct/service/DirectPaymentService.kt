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
package nl.nlportal.payment.direct.service

import com.onlinepayments.domain.AmountOfMoney
import com.onlinepayments.domain.CardPaymentMethodSpecificInputBase
import com.onlinepayments.domain.CreateHostedCheckoutRequest
import com.onlinepayments.domain.Feedbacks
import com.onlinepayments.domain.HostedCheckoutSpecificInput
import com.onlinepayments.domain.Order
import com.onlinepayments.domain.OrderReferences
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration.DirectPaymentProfile
import nl.nlportal.payment.direct.client.DirectPaymentClient
import nl.nlportal.payment.direct.domain.DirectPaymentCheckout
import nl.nlportal.payment.direct.domain.DirectPaymentException
import nl.nlportal.payment.direct.domain.DirectPaymentRequest
import nl.nlportal.payment.direct.domain.DirectPaymentResponse
import nl.nlportal.payment.direct.domain.DirectPaymentStatus
import nl.nlportal.payment.direct.domain.DirectPaymentStatusCategory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.net.URI

open class DirectPaymentService(
    private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
    private val directPaymentClient: DirectPaymentClient,
) {
    suspend fun getDirectPaymentStatus(
        identifier: String,
        hostedCheckoutId: String,
    ): DirectPaymentStatus =
        DirectPaymentStatus(
            status = getDirectPaymentCheckout(identifier, hostedCheckoutId).status,
        )

    /**
     * Get the hosted checkout from the payment provider.
     * @param identifier: payment profile key or pspId
     * @param hostedCheckoutId: id of the hosted checkout
     * @return orderId of the created payment, if any, and the payment status
     */
    suspend fun getDirectPaymentCheckout(
        identifier: String,
        hostedCheckoutId: String,
    ): DirectPaymentCheckout {
        val paymentDirectProfile =
            directPaymentModuleConfiguration.properties.getPaymentProfile(identifier)
                ?: directPaymentModuleConfiguration.properties.getPaymentProfileByPspPid(identifier)
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not found direct payment profile for the identifier $identifier",
                )

        val directPaymentStatusResponse =
            directPaymentClient.hostedCheckoutStatus(
                directPaymentProfile = paymentDirectProfile,
                hostedCheckoutId = hostedCheckoutId,
            )

        val createdPaymentOutput = directPaymentStatusResponse.createdPaymentOutput
        val checkout =
            DirectPaymentCheckout(
                orderId =
                    createdPaymentOutput
                        ?.payment
                        ?.paymentOutput
                        ?.references
                        ?.merchantReference,
                status = createdPaymentOutput?.paymentStatusCategory ?: DirectPaymentStatusCategory.STATUS_UNKNOWN,
            )
        logger.debug {
            "Hosted checkout $hostedCheckoutId for order ${checkout.orderId} has status ${directPaymentStatusResponse.status}, " +
                "payment status ${checkout.status}"
        }
        return checkout
    }

    /**
     * Verifies that the payment profile of the identifier belongs to the merchant.
     * @param identifier: payment profile key or pspId used for the payment
     * @param merchant: payment profile key or pspId of the merchant the order belongs to
     * @throws DirectPaymentException.MerchantMismatch when the identifier has no profile or another pspId than the merchant
     */
    fun verifyMerchant(
        identifier: String,
        merchant: String,
    ) {
        val pspId = findPaymentProfile(identifier)?.pspId
        if (pspId == null || pspId != findPaymentProfile(merchant)?.pspId) {
            throw DirectPaymentException.MerchantMismatch()
        }
    }

    private fun findPaymentProfile(identifier: String): DirectPaymentProfile? =
        directPaymentModuleConfiguration.properties.getPaymentProfile(identifier)
            ?: directPaymentModuleConfiguration.properties.getPaymentProfileByPspPid(identifier)

    /**
     * Do Direct Payment at payment provider. The returnUrl of the request is only used when the returnUrl of the payment
     * profile is an absolute URL with the same origin, otherwise the returnUrl of the payment profile is used.
     * @param paymentRequest: properties to create a payment
     * @return redirectUrl to do the actual payment at payment provider
     */
    suspend fun doDirectPayment(
        paymentRequest: DirectPaymentRequest,
    ): DirectPaymentResponse {
        val amount = toCents(paymentRequest.amount)
        try {
            val paymentDirectProfile =
                directPaymentModuleConfiguration.properties.getPaymentProfile(paymentRequest.identifier)
                    ?: directPaymentModuleConfiguration.properties.getPaymentProfileByPspPid(paymentRequest.identifier)
                    ?: throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Could not found direct payment profile for the identifier $paymentRequest.identifier",
                    )

            val hostedCheckoutSpecificInput =
                HostedCheckoutSpecificInput()
                    .withLocale(paymentRequest.langId ?: paymentDirectProfile.language)
                    .withReturnUrl(
                        paymentRequest.returnUrl?.takeIf {
                            runCatching { URI(paymentDirectProfile.returnUrl) }
                                .getOrNull()
                                ?.takeIf { profileUri -> profileUri.isAbsolute && profileUri.host != null }
                                ?.let { profileUri -> it.startsWith(profileUri.resolve("/").toString()) } == true
                        } ?: paymentDirectProfile.returnUrl,
                    ).withShowResultPage(directPaymentModuleConfiguration.properties.showResultPage)
                    .withAllowedNumberOfPaymentAttempts(paymentDirectProfile.allowedNumberOfPayments)

            directPaymentModuleConfiguration.properties.customTemplateUrl?.let {
                hostedCheckoutSpecificInput.variant = it
            }

            val checkoutRequest =
                CreateHostedCheckoutRequest()
                    .withOrder(
                        Order()
                            .withAmountOfMoney(
                                AmountOfMoney()
                                    .withAmount(amount)
                                    .withCurrencyCode(paymentDirectProfile.currency),
                            ).withReferences(
                                OrderReferences()
                                    .withDescriptor(paymentRequest.reference)
                                    .withMerchantReference(paymentRequest.orderId),
                            ),
                    ).withHostedCheckoutSpecificInput(
                        hostedCheckoutSpecificInput,
                    )

            // only enable creditcard support if creditcardEnabled is enabled
            if (paymentDirectProfile.creditcardEnabled) {
                checkoutRequest.withCardPaymentMethodSpecificInput(
                    CardPaymentMethodSpecificInputBase()
                        .withAuthorizationMode("SALE"),
                )
            }

            // if webhookUrl property is configured, set webhook url in the request. Benefit is dynamically set the webhook url per environment.
            directPaymentModuleConfiguration.properties.webhookUrl?.let {
                checkoutRequest.withFeedbacks(
                    Feedbacks().withWebhooksUrls(listOf(it)),
                )
            }

            val response =
                directPaymentClient.hostedCheckout(
                    directPaymentProfile = paymentDirectProfile,
                    checkoutRequest = checkoutRequest,
                )
            logger.debug { "Hosted checkout ${response.hostedCheckoutId} created for order ${paymentRequest.orderId}" }

            return DirectPaymentResponse(
                redirectUrl = response.redirectUrl,
            )
        } catch (ex: Exception) {
            logger.error(ex) { "Error while do payment for ${paymentRequest.identifier} - ${ex.message}" }
            throw ex
        }
    }

    companion object {
        private val logger: KLogger = KotlinLogging.logger {}

        /**
         * Converts an amount in euros to cents exactly. Amounts with more than 2 decimals are rejected.
         * @param amount: amount in euros
         * @return amount in cents
         * @throws DirectPaymentException.AmountNotConvertible when the amount can not be converted exactly
         */
        fun toCents(amount: Double): Long =
            try {
                BigDecimal.valueOf(amount).movePointRight(2).longValueExact()
            } catch (ex: ArithmeticException) {
                throw DirectPaymentException.AmountNotConvertible()
            }
    }
}