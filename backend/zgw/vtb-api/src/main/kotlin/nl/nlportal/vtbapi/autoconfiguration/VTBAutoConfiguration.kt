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
package nl.nlportal.vtbapi.autoconfiguration

import nl.nlportal.vtbapi.client.VTBClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(VTBModuleConfiguration::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["vtbapi.enabled"], havingValue = "true")
class VTBAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(VTBClient::class)
    fun vtbClient(
        vtbModuleConfiguration: VTBModuleConfiguration,
    ): VTBClient =
        VTBClient(
            vtbModuleConfigurationProperties = vtbModuleConfiguration.properties,
        )
}