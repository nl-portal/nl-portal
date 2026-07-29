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
package nl.nlportal.core.frontend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.frontend.configuration.FrontendFeaturesConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleFeaturesConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleFeaturesConfigurationProperties.FrontendModuleFeaturesProperties

class FrontendFeaturesConfigurationService(
    private var frontendFeaturesConfigurationProperties: FrontendFeaturesConfigurationProperties,
    private var frontendModuleFeaturesConfigurationProperties: FrontendModuleFeaturesConfigurationProperties,
) {
    fun getFeatures(): FrontendFeaturesConfigurationProperties {
        val features = frontendFeaturesConfigurationProperties
        features.config = getFeatureModulesEnabled()
        return frontendFeaturesConfigurationProperties
    }

    fun getFeatureModulesEnabled(): Map<String, FrontendModuleFeaturesProperties> {
        val excludedModules = frontendModuleFeaturesConfigurationProperties.excludedModules
        return frontendModuleFeaturesConfigurationProperties.config
            .filterKeys { it !in excludedModules }
            .mapKeys { (key, _) -> key.replace("api", "") }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}