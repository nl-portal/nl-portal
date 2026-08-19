/*
 * Copyright 2026 Ritense BV, the Netherlands.
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

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties.FrontendModuleProperties
import nl.nlportal.core.frontend.domain.FrontendData

class FrontendConfigurationService(
    private val frontendConfigurationProperties: FrontendConfigurationProperties,
    private val frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
) {
    fun getFrontendData(): FrontendData =
        FrontendData(
            modules = getModules(),
            toggles = frontendConfigurationProperties.toggles,
            properties = frontendConfigurationProperties.properties,
            theme = frontendConfigurationProperties.theme,
        )

    fun getModules(): Map<String, FrontendModuleProperties> {
        val modulesWhitelist = frontendConfigurationProperties.modulesWhitelist
        return frontendModuleConfigurationProperties.config
            .filterKeys { it in modulesWhitelist }
            .mapKeys { (key, _) -> key.replace("api", "") }
    }

    fun getLogo(): String? = frontendConfigurationProperties.theme.logo

    fun getStyle(): String? = frontendConfigurationProperties.theme.style
}