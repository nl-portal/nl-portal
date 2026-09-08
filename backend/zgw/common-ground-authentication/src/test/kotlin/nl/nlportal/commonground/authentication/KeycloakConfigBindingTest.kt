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
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import kotlin.test.assertTrue

internal class KeycloakConfigBindingTest {
    @Configuration
    @EnableConfigurationProperties(KeycloakConfig::class)
    internal class TestConfiguration

    private val runner = ApplicationContextRunner().withUserConfiguration(TestConfiguration::class.java)

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
        assertTrue(bind("audience" to "").audience.isBlank())
    }

    @Test
    fun `audience is blank when the property is absent`() {
        assertEquals("", bind().audience)
    }

    @Test
    fun `the whole app image configuration shape binds`() {
        val config =
            bind(
                "resource" to "nl-portal-m2m",
                "audience" to "",
                "credentials.secret" to "a-secret",
                "token-exchange-version" to "v2",
            )

        assertEquals("nl-portal-m2m", config.resource)
        assertEquals("", config.audience)
        assertEquals("a-secret", config.credentials.secret)
        assertEquals(KeycloakConfig.TokenExchangeVersion.V2, config.tokenExchangeVersion)
    }

    @Test
    fun `token-exchange-version binds from a relaxed uppercase environment variable`() {
        runner
            .withInitializer { context ->
                context.environment.propertySources.addFirst(
                    SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        mapOf<String, Any>("NL_PORTAL_AUTHENTICATION_KEYCLOAK_TOKEN_EXCHANGE_VERSION" to "v2"),
                    ),
                )
            }.run { context ->
                assertEquals(
                    KeycloakConfig.TokenExchangeVersion.V2,
                    context.getBean(KeycloakConfig::class.java).tokenExchangeVersion,
                )
            }
    }

    @Test
    fun `a higher precedence property source wins, as a config server source does`() {
        runner
            .withInitializer { context ->
                val sources = context.environment.propertySources
                sources.addLast(
                    MapPropertySource(
                        "applicationConfig: [classpath:/application.yml]",
                        mapOf("nl-portal.authentication.keycloak.token-exchange-version" to "v1"),
                    ),
                )
                sources.addFirst(
                    MapPropertySource(
                        "configserver:nl-portal-production",
                        mapOf("nl-portal.authentication.keycloak.token-exchange-version" to "v2"),
                    ),
                )
            }.run { context ->
                assertEquals(
                    KeycloakConfig.TokenExchangeVersion.V2,
                    context.getBean(KeycloakConfig::class.java).tokenExchangeVersion,
                )
            }
    }

    @Test
    fun `the constructor arities published in 3-0-5 still exist`() {
        val type = KeycloakConfig::class.java

        assertEquals(0, type.getDeclaredConstructor().parameterCount)
        assertEquals(1, type.getDeclaredConstructor(String::class.java).parameterCount)
        assertEquals(2, type.getDeclaredConstructor(String::class.java, String::class.java).parameterCount)
        assertEquals(
            3,
            type
                .getDeclaredConstructor(
                    String::class.java,
                    String::class.java,
                    KeycloakConfig.KeycloakCredentials::class.java,
                ).parameterCount,
        )
    }

    private fun bind(vararg properties: Pair<String, String>): KeycloakConfig {
        var config: KeycloakConfig? = null
        runner
            .withPropertyValues(
                *properties
                    .map { (key, value) -> "nl-portal.authentication.keycloak.$key=$value" }
                    .toTypedArray(),
            ).run { context -> config = context.getBean(KeycloakConfig::class.java) }
        return config!!
    }
}