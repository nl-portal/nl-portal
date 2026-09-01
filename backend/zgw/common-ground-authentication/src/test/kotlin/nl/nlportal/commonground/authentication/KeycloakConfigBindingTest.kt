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
package nl.nlportal.commonground.authentication

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import kotlin.test.assertTrue

internal class KeycloakConfigBindingTest {
    @Test
    fun `token-exchange-version binds the lowercase yaml literals to the enum`() {
        assertEquals(KeycloakConfig.TokenExchangeVersion.V1, bind("token-exchange-version" to "v1").tokenExchangeVersion)
        assertEquals(KeycloakConfig.TokenExchangeVersion.V2, bind("token-exchange-version" to "v2").tokenExchangeVersion)
        assertEquals(KeycloakConfig.TokenExchangeVersion.V2, bind("token-exchange-version" to "V2").tokenExchangeVersion)
    }

    @Test
    fun `token-exchange-version defaults to V1 when absent`() {
        assertEquals(KeycloakConfig.TokenExchangeVersion.V1, bind().tokenExchangeVersion)
    }

    @Test
    fun `an unresolved audience placeholder default binds as blank, not as a literal`() {
        val config = bind("audience" to "")

        assertTrue(config.audience.isNullOrBlank())
    }

    @Test
    fun `audience is null when the property is absent`() {
        assertEquals(null, bind().audience)
    }

    private fun bind(vararg properties: Pair<String, String>): KeycloakConfig {
        val source =
            MapConfigurationPropertySource(
                properties.associate { (key, value) -> "nl-portal.authentication.keycloak.$key" to value },
            )
        return Binder(source)
            .bind("nl-portal.authentication.keycloak", KeycloakConfig::class.java)
            .orElseGet { KeycloakConfig() }
    }
}