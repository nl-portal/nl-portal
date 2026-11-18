/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
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
package nl.nlportal.payment.autoconfiguration

import nl.nlportal.payment.api.OgonePaymentController
import nl.nlportal.payment.graphql.OgonePaymentMutation
import nl.nlportal.payment.service.OgonePaymentService
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.taak.service.TaakService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@Deprecated("Ogone e-Commerce is no longer supported and will be removed in 4.0.0. Use the payment-direct module instead.")
@AutoConfiguration
@EnableConfigurationProperties(OgonePaymentConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled", "taak.enabled", "payment.ogone.enabled"], havingValue = "true")
class PaymentAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(OgonePaymentService::class)
    fun ogonePaymentService(
        ogonePaymentConfig: OgonePaymentConfig,
        objectsApiClient: ObjectsApiClient,
    ): OgonePaymentService =
        OgonePaymentService(
            ogonePaymentConfig.properties,
            objectsApiClient,
        )

    @Bean
    fun ogonePaymentMutation(
        ogonePaymentService: OgonePaymentService,
        taakService: TaakService,
        ogonePaymentConfig: OgonePaymentConfig,
    ): OgonePaymentMutation = OgonePaymentMutation(ogonePaymentService, taakService, ogonePaymentConfig.properties)

    @Bean
    @ConditionalOnMissingBean(OgonePaymentController::class)
    fun ogonePaymentController(ogonePaymentService: OgonePaymentService): OgonePaymentController = OgonePaymentController(ogonePaymentService)
}