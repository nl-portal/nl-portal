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
package nl.nlportal.zakenapi.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource

class ZakenApiConfigBindingTest {
    @Test
    fun `binds use-nnp-kvk-query-identificators from the canonical property name`() {
        val config =
            bind(
                mapOf(
                    "nl-portal.config.zakenapi.properties.use-nnp-kvk-query-identificators" to "true",
                ),
            )

        assertTrue(config.properties.useNnpKvkQueryIdentificators)
    }

    @Test
    fun `binds use-nnp-kvk-query-identificators from the environment variable name`() {
        val config =
            bind(
                mapOf(
                    "NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_USENNPKVKQUERYIDENTIFICATORS" to "true",
                ),
            )

        assertTrue(config.properties.useNnpKvkQueryIdentificators)
    }

    @Test
    fun `binds the surrounding properties from environment variable names`() {
        val config =
            bind(
                mapOf(
                    "NLPORTAL_CONFIG_ZAKENAPI_ENABLED" to "true",
                    "NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_URL" to "http://localhost:8001",
                    "NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_CLIENTID" to "nl-portal",
                    "NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_USENNPKVKQUERYIDENTIFICATORS" to "true",
                ),
            )

        assertTrue(config.enabled)
        assertEquals("http://localhost:8001", config.properties.url)
        assertEquals("nl-portal", config.properties.clientId)
        assertTrue(config.properties.useNnpKvkQueryIdentificators)
    }

    private fun bind(source: Map<String, Any>): ZakenApiConfig {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            SystemEnvironmentPropertySource("test-systemEnvironment", source),
        )
        return Binder.get(environment)
            .bind("nl-portal.config.zakenapi", ZakenApiConfig::class.java)
            .orElseThrow { AssertionError("nl-portal.config.zakenapi did not bind") }
    }
}
