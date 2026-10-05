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
import java.util.UUID
import nl.nlportal.core.util.Mapper
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.constants.DirectPaymentState
import nl.nlportal.payment.direct.domain.DirectPaymentWebhookRequest
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.domain.ObjectsApiObject
import nl.nlportal.zgw.objectenapi.domain.UpdateObjectsApiObjectRequest
import nl.nlportal.zgw.taak.domain.TaakObjectV2
import nl.nlportal.zgw.taak.domain.TaakStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class DirectPaymentWebhookService(
    private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
    private val objectsApiClient: ObjectsApiClient,
) {
    val webhookHelper = WebhooksHelper(DefaultMarshaller.INSTANCE, InMemorySecretKeyStore.INSTANCE)

    init {
        directPaymentModuleConfiguration.properties.configurations.forEach {
            InMemorySecretKeyStore.INSTANCE.storeSecretKey(it.value.webhookApiKey, it.value.webhookApiSecret)
        }
    }

    /**
     * Handle Postsale webhook server to server call from payment provider
     * @param: httpHeaders, headers from the request
     * @param: jsonBody, the raw body of the request
     *
     * @return: information which will used in the response of the webhook
     * @throws: ResponseStatusException is some check is not valid
     */
    suspend fun handlePostSale(
        httpHeaders: HttpHeaders,
        jsonBody: String,
    ): String {
        if (!isValidDirectRequest(httpHeaders, jsonBody)) {
            return "Request is not valid"
        }
        val directPaymentWebhookRequest =
            Mapper.get().readValue(
                jsonBody,
                DirectPaymentWebhookRequest::class.java,
            )
        val orderId = directPaymentWebhookRequest.payment.paymentOutput.references.merchantReference
        if (isUUID(orderId)) {
            val status = directPaymentWebhookRequest.payment.statusOutput.statusCode
            if (status != DirectPaymentState.SUCCESS.status &&
                status != DirectPaymentState.PENDING.status &&
                status != DirectPaymentState.PENDING1.status &&
                status != DirectPaymentState.PENDING2.status
            ) {
                return "Request has not the correct status: $status"
            }

            val objectsApiTask = getObjectsApiTaak(UUID.fromString(orderId))
            if (objectsApiTask.record.data.status != TaakStatus.OPEN) {
                return "Task is already completed"
            }

            val updateRequest = UpdateObjectsApiObjectRequest.fromObjectsApiObject(objectsApiTask)
            updateRequest.record.data.status = TaakStatus.AFGEROND
            updateRequest.record.correctedBy = "Payment provider"
            updateRequest.record.correctionFor = objectsApiTask.record.index.toString()
            objectsApiClient.updateObject(objectsApiTask.uuid, updateRequest)
        }
        return "Request successful processed for order $orderId"
    }

    private suspend fun getObjectsApiTaak(taskId: UUID): ObjectsApiObject<TaakObjectV2> {
        val objectsApiTask = objectsApiClient.getObjectById<TaakObjectV2>(taskId.toString())
        if (objectsApiTask == null) {
            throw ResponseStatusException(
                HttpStatus.OK,
                String.format("Taak kan niet gevonden worden", taskId),
            )
        }
        return objectsApiTask
    }

    private fun isValidDirectRequest(
        httpHeaders: HttpHeaders,
        bodyOfRequest: String,
    ): Boolean {
        val requestHeaders =
            listOf<RequestHeader>(
                RequestHeader(HEADER_X_GCS_SIGNATURE, httpHeaders[HEADER_X_GCS_SIGNATURE]?.get(0)),
                RequestHeader(HEADER_X_GCS_KEYID, httpHeaders[HEADER_X_GCS_KEYID]?.get(0)),
            )

        try {
            webhookHelper.unmarshal(bodyOfRequest, requestHeaders)
            return true
        } catch (e: SignatureValidationException) {
            return false
        }
    }

    companion object {
        const val HEADER_X_GCS_SIGNATURE: String = "X-GCS-Signature"
        const val HEADER_X_GCS_KEYID: String = "X-GCS-KeyId"

        fun isUUID(orderId: String?): Boolean =
            try {
                UUID.fromString(orderId) != null
            } catch (e: IllegalArgumentException) {
                false
            }
    }
}