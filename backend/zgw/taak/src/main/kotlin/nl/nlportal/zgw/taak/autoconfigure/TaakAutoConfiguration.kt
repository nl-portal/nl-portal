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
package nl.nlportal.zgw.taak.autoconfigure

import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.core.security.config.HttpSecurityConfigurer
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.documentenapi.service.VirusScanService
import nl.nlportal.form.service.ObjectsApiFormDefinitionService
import nl.nlportal.payment.direct.service.DirectPaymentService
import nl.nlportal.payment.direct.service.DirectPaymentWebhookService
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.taak.graphql.TaakFormQuery
import nl.nlportal.zgw.taak.graphql.TaakMutationV2
import nl.nlportal.zgw.taak.graphql.TaakPaymentMutation
import nl.nlportal.zgw.taak.graphql.TaakPaymentQuery
import nl.nlportal.zgw.taak.graphql.TaakQueryV2
import nl.nlportal.zgw.taak.security.config.TaakDocumentResourceHttpSecurityConfigurer
import nl.nlportal.zgw.taak.service.TaakPaymentService
import nl.nlportal.zgw.taak.service.TaakService
import nl.nlportal.zgw.taak.web.rest.TaakDocumentResource
import nl.nlportal.zgw.taak.web.rest.TaakPaymentWebhookResource
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(TaakConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled", "taak.enabled"], havingValue = "true")
class TaakAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(TaakService::class)
    fun taskService(
        objectsApiClient: ObjectsApiClient,
        taakObjectConfig: TaakConfig,
        authenticationMachtigingsService: AuthenticationMachtigingsDienstService,
    ): TaakService =
        TaakService(
            objectsApiClient = objectsApiClient,
            taakConfigProperties = taakObjectConfig.properties,
            authenticationMachtigingsDienstService = authenticationMachtigingsService,
        )

    @Bean
    @ConditionalOnMissingBean(TaakQueryV2::class)
    fun taskQueryV2(taskService: TaakService): TaakQueryV2 = TaakQueryV2(taskService)

    @Bean
    @ConditionalOnMissingBean(TaakMutationV2::class)
    fun taskMutationV2(taskService: TaakService): TaakMutationV2 = TaakMutationV2(taskService)

    @Bean
    @ConditionalOnMissingBean(TaakFormQuery::class)
    fun taakFormQuery(
        taskService: TaakService,
        objectsApiFormDefinitionService: ObjectsApiFormDefinitionService,
    ): TaakFormQuery = TaakFormQuery(taskService, objectsApiFormDefinitionService)

    @Bean
    @ConditionalOnMissingBean(TaakDocumentResource::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.documentenapis", name = ["enabled"], havingValue = "true")
    fun taakDocumentUploadResource(
        taakService: TaakService,
        documentenApiService: DocumentenApiService,
        virusScanService: VirusScanService?,
    ): TaakDocumentResource = TaakDocumentResource(taakService, documentenApiService, virusScanService)

    @Bean
    @ConditionalOnMissingBean(TaakDocumentResourceHttpSecurityConfigurer::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.documentenapis", name = ["enabled"], havingValue = "true")
    fun taakDocumentResourceHttpSecurityConfigurer(): HttpSecurityConfigurer = TaakDocumentResourceHttpSecurityConfigurer()

    @Bean
    @ConditionalOnMissingBean(TaakPaymentService::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.payment.direct", name = ["enabled"], havingValue = "true")
    fun taakPaymentService(
        taakService: TaakService,
        objectsApiClient: ObjectsApiClient,
        directPaymentService: DirectPaymentService,
        directPaymentWebhookService: DirectPaymentWebhookService,
    ): TaakPaymentService =
        TaakPaymentService(
            taakService = taakService,
            objectsApiClient = objectsApiClient,
            directPaymentService = directPaymentService,
            directPaymentWebhookService = directPaymentWebhookService,
        )

    @Bean
    @ConditionalOnMissingBean(TaakPaymentMutation::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.payment.direct", name = ["enabled"], havingValue = "true")
    fun taakPaymentMutation(
        taakPaymentService: TaakPaymentService,
    ): TaakPaymentMutation =
        TaakPaymentMutation(
            taakPaymentService = taakPaymentService,
        )

    @Bean
    @ConditionalOnMissingBean(TaakPaymentQuery::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.payment.direct", name = ["enabled"], havingValue = "true")
    fun taakPaymentQuery(
        taakPaymentService: TaakPaymentService,
    ): TaakPaymentQuery =
        TaakPaymentQuery(
            taakPaymentService = taakPaymentService,
        )

    @Bean
    @ConditionalOnMissingBean(TaakPaymentWebhookResource::class)
    @ConditionalOnProperty(prefix = "nl-portal.config.payment.direct", name = ["enabled"], havingValue = "true")
    fun taakPaymentWebhookResource(
        taakPaymentService: TaakPaymentService,
    ): TaakPaymentWebhookResource =
        TaakPaymentWebhookResource(
            taakPaymentService = taakPaymentService,
        )
}