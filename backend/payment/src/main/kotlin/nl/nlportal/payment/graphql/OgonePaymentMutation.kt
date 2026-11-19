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

import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.payment.autoconfiguration.OgonePaymentConfig.OgonePaymentConfigProperties
import nl.nlportal.payment.domain.OgonePayment
import nl.nlportal.payment.domain.OgonePaymentRequest
import nl.nlportal.payment.service.OgonePaymentService
import nl.nlportal.zgw.taak.domain.TaakStatus
import nl.nlportal.zgw.taak.service.TaakService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Generates an Ogone payment for an open payment taak of the logged-in user. The orderId must be the id of that taak
 * and the pspId must resolve to the payment profile of the taak. The amount, reference and title of the request are
 * ignored, they are taken from the taak.
 */
@Deprecated("Ogone e-Commerce is no longer supported and will be removed in 4.0.0. Use the payment-direct module instead.")
@Controller
class OgonePaymentMutation(
    private val ogonePaymentService: OgonePaymentService,
    private val taakService: TaakService,
    private val paymentConfig: OgonePaymentConfigProperties,
) {
    @MutationMapping
    suspend fun generateOgonePayment(
        authentication: CommonGroundAuthentication,
        @Argument paymentRequest: OgonePaymentRequest,
    ): OgonePayment {
        val taakId =
            runCatching { UUID.fromString(paymentRequest.orderId) }.getOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Taak payment not found")
        val taak = taakService.getTaakByIdV2(id = taakId, authentication = authentication)
        val betaling =
            taak.ogonebetaling?.takeIf { taak.status == TaakStatus.OPEN }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Taak payment not found")
        val profile = paymentConfig.findPaymentProfile(paymentRequest.pspId)
        if (profile == null || profile.pspId != paymentConfig.findPaymentProfile(betaling.pspid)?.pspId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment profile does not match the taak")
        }
        return ogonePaymentService.createPayment(
            paymentRequest.copy(
                amount = betaling.bedrag,
                reference = betaling.betaalkenmerk,
                orderId = taak.id.toString(),
                title = taak.titel,
            ),
        )
    }
}