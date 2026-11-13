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
package nl.nlportal.ikoapi.autoconfigure

import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.core.ssl.ResourceClientSslContextResolver
import nl.nlportal.ikoapi.client.IkoApiClient
import nl.nlportal.ikoapi.portalpage.IkoPortalPageBindingResolver
import nl.nlportal.ikoapi.service.IkoApiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
@EnableConfigurationProperties(IkoApiModuleConfiguration::class)
@ConditionalOnProperty(prefix = "nl-portal.config.ikoapi", name = ["enabled"], havingValue = "true")
class IkoApiAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ClientSslContextResolver::class)
    fun clientSslContextResolver(resourceLoader: ResourceLoader): ClientSslContextResolver = ResourceClientSslContextResolver(resourceLoader)

    @Bean
    @ConditionalOnMissingBean(IkoApiClient::class)
    fun ikoApiClient(
        ikoApiModuleConfiguration: IkoApiModuleConfiguration,
        webClientBuilder: WebClient.Builder,
        @Autowired(required = false) clientSslContextResolver: ClientSslContextResolver? = null,
    ): IkoApiClient = IkoApiClient.create(ikoApiModuleConfiguration.properties, webClientBuilder, clientSslContextResolver)

    @Bean
    @ConditionalOnMissingBean(IkoApiService::class)
    fun ikoApiService(
        ikoApiClient: IkoApiClient,
        ikoApiModuleConfiguration: IkoApiModuleConfiguration,
    ): IkoApiService = IkoApiService(ikoApiClient, ikoApiModuleConfiguration.properties)

    @Bean
    @ConditionalOnMissingBean(IkoPortalPageBindingResolver::class)
    fun ikoPortalPageBindingResolver(ikoApiService: IkoApiService): IkoPortalPageBindingResolver = IkoPortalPageBindingResolver(ikoApiService)
}