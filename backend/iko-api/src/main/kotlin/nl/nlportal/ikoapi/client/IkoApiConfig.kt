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
package nl.nlportal.ikoapi.client

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "nl-portal.config.ikoapi")
class IkoApiConfig {
    var enabled: Boolean = false
    var properties: IkoApiConfigProperties = IkoApiConfigProperties()

    class IkoApiConfigProperties {
        var url: URI? = null
        var tokenUrl: URI? = null
        var clientId: String? = null
        var clientSecret: String? = null
        var allowedProfiles: List<String> = emptyList()
    }
}