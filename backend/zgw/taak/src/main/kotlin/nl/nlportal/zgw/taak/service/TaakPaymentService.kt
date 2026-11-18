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

import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.payment.direct.domain.DirectPaymentRequest
import nl.nlportal.payment.direct.domain.DirectPaymentResponse
import nl.nlportal.payment.direct.domain.DirectPaymentStatus
import nl.nlportal.payment.direct.service.DirectPaymentService
import nl.nlportal.zgw.taak.domain.TaakPaymentRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class TaakPaymentService(
    private val taakService: TaakService,
    private val directPaymentService: DirectPaymentService,
) {
    suspend fun doTaakPayment(
        authentication: CommonGroundAuthentication,
        taakPaymentRequest: TaakPaymentRequest,
        returnUrl: String? = null,
    ): DirectPaymentResponse {
        val taak =
            taakService.getTaakByIdV2(
                authentication = authentication,
                id = taakPaymentRequest.taakId,
            )

        if (taak.ogonebetaling == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Taak payment not found")
        }

        val directPaymentRequest =
            DirectPaymentRequest(
                identifier = taakPaymentRequest.identifier,
                amount = taak.ogonebetaling.bedrag,
                reference = taak.ogonebetaling.betaalkenmerk,
                orderId = taakPaymentRequest.taakId.toString(),
                langId = taakPaymentRequest.langId,
                returnUrl = returnUrl,
            )

        return directPaymentService.doDirectPayment(
            paymentRequest = directPaymentRequest,
        )
    }

    suspend fun getTaakPaymentStatus(
        authentication: CommonGroundAuthentication,
        identifier: String,
        hostedCheckoutId: String,
    ): DirectPaymentStatus =
        directPaymentService.getDirectPaymentStatus(
            identifier = identifier,
            hostedCheckoutId = hostedCheckoutId,
        )
}