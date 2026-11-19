/*
 * Copyright 2025 Ritense BV, the Netherlands.
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
package nl.nlportal.payment.direct.graphql

import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.domain.DirectPaymentStatus
import nl.nlportal.payment.direct.service.DirectPaymentService
import nl.nlportal.zgw.taak.service.TaakService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Returns the status of a hosted checkout only when it belongs to a payment taak of the logged-in user.
 */
@Controller
class DirectPaymentQuery(
    private val directPaymentService: DirectPaymentService,
    private val taakService: TaakService,
    private val directPaymentModuleConfiguration: DirectPaymentModuleConfiguration,
) {
    @QueryMapping
    suspend fun getDirectPaymentStatus(
        authentication: CommonGroundAuthentication,
        @Argument identifier: String,
        @Argument hostedCheckoutId: String,
    ): DirectPaymentStatus {
        val (profile, response) = directPaymentService.getDirectPaymentStatusResponse(identifier, hostedCheckoutId)
        val taakId =
            response.createdPaymentOutput.payment
                ?.paymentOutput
                ?.references
                ?.merchantReference
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
        val taak = taakService.getTaakByIdV2(id = taakId, authentication = authentication)
        if (directPaymentModuleConfiguration.properties.findPaymentProfile(taak.ogonebetaling?.pspid)?.pspId != profile.pspId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")
        }
        return DirectPaymentStatus(status = response.createdPaymentOutput.paymentStatusCategory)
    }
}