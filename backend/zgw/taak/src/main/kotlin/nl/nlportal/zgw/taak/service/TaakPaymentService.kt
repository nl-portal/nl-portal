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
package nl.nlportal.zgw.taak.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.payment.direct.domain.DirectPaymentException
import nl.nlportal.payment.direct.domain.DirectPaymentRequest
import nl.nlportal.payment.direct.domain.DirectPaymentResponse
import nl.nlportal.payment.direct.domain.DirectPaymentStatus
import nl.nlportal.payment.direct.service.DirectPaymentService
import nl.nlportal.payment.direct.service.DirectPaymentWebhookService
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.domain.ObjectsApiObject
import nl.nlportal.zgw.objectenapi.domain.UpdateObjectsApiObjectRequest
import nl.nlportal.zgw.taak.domain.TaakObjectV2
import nl.nlportal.zgw.taak.domain.TaakPaymentRequest
import nl.nlportal.zgw.taak.domain.TaakStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class TaakPaymentService(
    private val taakService: TaakService,
    private val objectsApiClient: ObjectsApiClient,
    private val directPaymentService: DirectPaymentService,
    private val directPaymentWebhookService: DirectPaymentWebhookService,
) {
    /**
     * Do Direct Payment for an open payment taak of the authenticated user. Amount, reference and orderId are taken from the taak.
     * @param authentication: the authenticated user
     * @param taakPaymentRequest: payment profile identifier, taak id, language and return url
     * @return redirectUrl to do the actual payment at payment provider
     * @throws ResponseStatusException with BAD_REQUEST when the taak is not an open payment taak or the payment profile does not match the taak
     */
    suspend fun doTaakPayment(
        authentication: CommonGroundAuthentication,
        taakPaymentRequest: TaakPaymentRequest,
    ): DirectPaymentResponse {
        val taakId = taakPaymentRequest.taakId
        val taak =
            taakService.getTaakByIdV2(
                authentication = authentication,
                id = taakId,
            )

        if (taak.status != TaakStatus.OPEN || taak.ogonebetaling == null) {
            logger.warn { "Payment for taak $taakId rejected: taak is not an open payment taak" }
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Taak payment not found")
        }

        try {
            directPaymentService.verifyMerchant(taakPaymentRequest.identifier, taak.ogonebetaling.pspid)
            return directPaymentService.doDirectPayment(
                paymentRequest =
                    DirectPaymentRequest(
                        identifier = taakPaymentRequest.identifier,
                        amount = taak.ogonebetaling.bedrag,
                        reference = taak.ogonebetaling.betaalkenmerk,
                        orderId = taakId.toString(),
                        langId = taakPaymentRequest.langId,
                        returnUrl = taakPaymentRequest.returnUrl,
                    ),
            )
        } catch (ex: DirectPaymentException.MerchantMismatch) {
            logger.warn { "Payment for taak $taakId rejected: ${ex.message}" }
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment profile does not match the taak")
        } catch (ex: DirectPaymentException.AmountNotConvertible) {
            logger.warn { "Payment for taak $taakId rejected: ${ex.message}" }
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount can not be converted to cents")
        }
    }

    /**
     * Get the status of a hosted checkout for a payment taak the authenticated user may act on.
     * @param authentication: the authenticated user
     * @param identifier: payment profile key or pspId
     * @param hostedCheckoutId: id of the hosted checkout
     * @return status of the payment
     * @throws ResponseStatusException with NOT_FOUND when the checkout does not belong to such a taak of the same merchant
     */
    suspend fun getTaakPaymentStatus(
        authentication: CommonGroundAuthentication,
        identifier: String,
        hostedCheckoutId: String,
    ): DirectPaymentStatus {
        val checkout =
            directPaymentService.getDirectPaymentCheckout(
                identifier = identifier,
                hostedCheckoutId = hostedCheckoutId,
            )
        if (checkout.orderId == null) {
            logger.debug { "Payment status skipped: checkout has no payment yet" }
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
        }
        val taakId =
            runCatching { UUID.fromString(checkout.orderId) }.getOrNull()
                ?: run {
                    logger.warn { "Payment status rejected: checkout is not for a taak" }
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
                }
        val pspid =
            taakService.getTaakByIdV2(id = taakId, authentication = authentication).ogonebetaling?.pspid
                ?: run {
                    logger.warn { "Payment status for taak $taakId rejected: taak has no payment" }
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
                }
        try {
            directPaymentService.verifyMerchant(identifier, pspid)
        } catch (ex: DirectPaymentException.MerchantMismatch) {
            logger.warn { "Payment status for taak $taakId rejected: ${ex.message}" }
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
        }

        return DirectPaymentStatus(status = checkout.status)
    }

    /**
     * Handle Postsale webhook server to server call from payment provider. Completes the taak on a successful payment of
     * the taak amount to the taak merchant.
     * @param httpHeaders: headers from the request
     * @param jsonBody: the raw body of the request
     * @return information which will used in the response of the webhook
     */
    suspend fun handlePostSale(
        httpHeaders: HttpHeaders,
        jsonBody: String,
    ): String {
        val event =
            try {
                directPaymentWebhookService.parsePostSale(httpHeaders, jsonBody)
            } catch (ex: DirectPaymentException.InvalidSignature) {
                logger.warn { "Payment webhook rejected: ${ex.message}" }
                return "Request is not valid"
            } catch (ex: DirectPaymentException.PaymentNotFinal) {
                logger.debug { "Payment webhook skipped: ${ex.message}" }
                return "Request has not the correct status: ${ex.status}"
            }
        val orderId = event.orderId
        val taakId = runCatching { UUID.fromString(orderId) }.getOrNull()
        if (taakId == null) {
            logger.debug { "Payment webhook skipped: order is not a taak" }
            return "Request successful processed for order $orderId"
        }

        val objectsApiTask = getObjectsApiTaak(taakId)
        if (objectsApiTask.record.data.status != TaakStatus.OPEN) {
            logger.debug { "Payment webhook for taak $taakId skipped: taak is already completed" }
            return "Task is already completed"
        }
        val ogonebetaling =
            objectsApiTask.record.data.ogonebetaling
                ?: run {
                    logger.warn { "Payment webhook for taak $taakId rejected: taak has no payment" }
                    return "Task payment not found"
                }

        try {
            directPaymentWebhookService.verifyPayment(event, ogonebetaling.pspid, ogonebetaling.bedrag)
        } catch (ex: DirectPaymentException.MerchantMismatch) {
            logger.warn { "Payment webhook for taak $taakId rejected: ${ex.message}" }
            return "Merchant does not match the task"
        } catch (ex: DirectPaymentException.AmountMismatch) {
            logger.warn { "Payment webhook for taak $taakId rejected: ${ex.message}" }
            return "Amount is not the same as the amount in the task"
        }

        val updateRequest = UpdateObjectsApiObjectRequest.fromObjectsApiObject(objectsApiTask)
        updateRequest.record.data.status = TaakStatus.AFGEROND
        updateRequest.record.correctedBy = "Payment provider"
        updateRequest.record.correctionFor = objectsApiTask.record.index.toString()
        objectsApiClient.updateObject(objectsApiTask.uuid, updateRequest)
        logger.info { "Payment webhook completed taak $taakId" }
        return "Request successful processed for order $orderId"
    }

    private suspend fun getObjectsApiTaak(taskId: UUID): ObjectsApiObject<TaakObjectV2> =
        objectsApiClient.getObjectById<TaakObjectV2>(taskId.toString())
            ?: throw ResponseStatusException(HttpStatus.OK, "Taak kan niet gevonden worden")

    companion object {
        private val logger: KLogger = KotlinLogging.logger {}
    }
}