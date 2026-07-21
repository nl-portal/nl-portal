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
package nl.nlportal.zaken.autoconfigure

import nl.nlportal.catalogiapi.client.CatalogiApiConfig
import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.ZakenConfig
import nl.nlportal.zaken.service.ZakenService
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient.Builder

@AutoConfiguration
@EnableConfigurationProperties(
    ZakenConfig::class,
)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled","catalogiapi.enabled","zaken.enabled"], havingValue = "true")
class ZakenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ZakenService::class)
    fun zakenService(
        zakenConfig: ZakenConfig,
        zakenClient: ZakenClient,
        documentenApiService: DocumentenApiService,
        objectsApiClient: ObjectsApiClient,
        authenticationMachtigingsDienstService: AuthenticationMachtigingsDienstService,
    ): ZakenService {
        return ZakenService(
            zakenApiClient = zakenClient,
            zakenApiConfigProperties = zakenConfig.properties,
            documentenApiService = documentenApiService,
            objectsApiClient = objectsApiClient,
            authenticationMachtigingsDienstService = authenticationMachtigingsDienstService,
        )
    }

    @Bean
    fun zakenConfig(): ZakenConfig {
        return ZakenConfig()
    }

    @Bean
    fun zakenClient(
        zakenConfig: ZakenConfig,
        catalogiApiConfig: CatalogiApiConfig,
        webClientBuilder: Builder,
    ): ZakenClient {
        return ZakenClient(zakenConfig.properties, catalogiApiConfig.properties, webClientBuilder)
    }
}
