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

import nl.nlportal.besluiten.service.BesluitenService
import nl.nlportal.catalogiapi.service.CatalogiApiService
import nl.nlportal.core.security.config.HttpSecurityConfigurer
import nl.nlportal.zaken.graphql.ZaakQuery
import nl.nlportal.zaken.security.config.ZaakDocumentResourceHttpSecurityConfigurer
import nl.nlportal.zakenapi.service.ZakenApiService
import nl.nlportal.zaken.web.rest.ZaakDocumentResource
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled","catalogiapi.enabled","zakenapi.enabled"], havingValue = "true")
class ZakenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ZaakQuery::class)
    fun zaakQuery(
        zakenApiService: ZakenApiService,
        besluitenService: BesluitenService,
        catalogiApiService: CatalogiApiService,
    ): ZaakQuery {
        return ZaakQuery(
            zakenApiService = zakenApiService,
            besluitenService = besluitenService,
            catalogiApiService = catalogiApiService
        )
    }

    @Bean
    @ConditionalOnMissingBean(ZaakDocumentResource::class)
    fun zaakDocumentResource(zakenApiService: ZakenApiService): ZaakDocumentResource {
        return ZaakDocumentResource(zakenApiService)
    }

    @Bean
    @ConditionalOnMissingBean(ZaakDocumentResourceHttpSecurityConfigurer::class)
    fun zaakDocumentResourceHttpSecurityConfigurer(): HttpSecurityConfigurer {
        return ZaakDocumentResourceHttpSecurityConfigurer()
    }
}
