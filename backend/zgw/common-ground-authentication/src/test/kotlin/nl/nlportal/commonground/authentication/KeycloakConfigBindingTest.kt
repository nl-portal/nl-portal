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
package nl.nlportal.commonground.authentication

import nl.nlportal.commonground.authentication.KeycloakConfig.KeycloakCredentials
import nl.nlportal.commonground.authentication.KeycloakConfig.TokenExchangeVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.SystemEnvironmentPropertySource

@Suppress("DEPRECATION")
internal class KeycloakConfigBindingTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration::class.java))

    @Test
    fun `token-exchange-version binds the lowercase yaml literals to the enum`() {
        bind("nl-portal.authentication.keycloak.token-exchange-version=v1") {
            assertEquals(TokenExchangeVersion.V1, it.tokenExchangeVersion)
        }
        bind("nl-portal.authentication.keycloak.token-exchange-version=v2") {
            assertEquals(TokenExchangeVersion.V2, it.tokenExchangeVersion)
        }
        bind("nl-portal.authentication.keycloak.token-exchange-version=V2") {
            assertEquals(TokenExchangeVersion.V2, it.tokenExchangeVersion)
        }
    }

    @Test
    fun `token-exchange-version defaults to V1 when absent`() {
        bind("nl-portal.authentication.keycloak.resource=nl-portal-m2m") {
            assertEquals(TokenExchangeVersion.V1, it.tokenExchangeVersion)
        }
    }

    @Test
    fun `an unresolved audience placeholder default binds as blank, not as a literal`() {
        bind("nl-portal.authentication.keycloak.audience=") {
            assertEquals("", it.audience)
        }
    }

    @Test
    fun `audience is blank when the property is absent`() {
        bind("nl-portal.authentication.keycloak.resource=nl-portal-m2m") {
            assertEquals("", it.audience)
        }
    }

    @Test
    fun `the whole app image configuration shape binds`() {
        bind(
            "nl-portal.authentication.keycloak.token-exchange-version=v2",
            "nl-portal.authentication.keycloak.resource=nl-portal-m2m",
            "nl-portal.authentication.keycloak.audience=",
            "nl-portal.authentication.keycloak.credentials.secret=a-secret",
        ) {
            assertEquals(TokenExchangeVersion.V2, it.tokenExchangeVersion)
            assertEquals("nl-portal-m2m", it.resource)
            assertEquals("", it.audience)
            assertEquals("a-secret", it.credentials.secret)
        }
    }

    @Test
    fun `token-exchange-version binds from a relaxed uppercase environment variable`() {
        assertBindsFromEnvironmentVariable(
            "NL_PORTAL_AUTHENTICATION_KEYCLOAK_TOKEN_EXCHANGE_VERSION",
            TokenExchangeVersion.V2,
        )
    }

    @Test
    fun `token-exchange-version binds from the hyphen-stripped environment variable form`() {
        assertBindsFromEnvironmentVariable(
            "NLPORTAL_AUTHENTICATION_KEYCLOAK_TOKENEXCHANGEVERSION",
            TokenExchangeVersion.V2,
        )
    }

    @Test
    fun `a mixed environment variable spelling does not bind and falls back to the default`() {
        assertBindsFromEnvironmentVariable(
            "NLPORTAL_AUTHENTICATION_KEYCLOAK_TOKEN_EXCHANGE_VERSION",
            TokenExchangeVersion.V1,
        )
        assertBindsFromEnvironmentVariable(
            "NL_PORTAL_AUTHENTICATION_KEYCLOAK_TOKENEXCHANGEVERSION",
            TokenExchangeVersion.V1,
        )
    }

    @Test
    fun `a higher precedence property source wins, as a config server source does`() {
        contextRunner
            .withInitializer { context ->
                context.environment.propertySources.addLast(
                    MapPropertySource(
                        "applicationConfig: [classpath:/application.yml]",
                        mapOf<String, Any>("nl-portal.authentication.keycloak.token-exchange-version" to "v1"),
                    ),
                )
                context.environment.propertySources.addFirst(
                    MapPropertySource(
                        "configserver:nl-portal-production",
                        mapOf<String, Any>("nl-portal.authentication.keycloak.token-exchange-version" to "v2"),
                    ),
                )
            }.run { context ->
                assertEquals(
                    TokenExchangeVersion.V2,
                    context.getBean(KeycloakConfig::class.java).tokenExchangeVersion,
                )
            }
    }

    @Test
    fun `the constructors that existed before the version switch are preserved`() {
        val parameterTypes =
            KeycloakConfig::class.java.constructors
                .map { constructor -> constructor.parameterTypes.toList() }
                .toSet()

        assertTrue(parameterTypes.contains(emptyList()), "no-arg constructor is missing")
        assertTrue(
            parameterTypes.contains(
                listOf(String::class.java, String::class.java, KeycloakCredentials::class.java),
            ),
            "the three argument constructor published before the version switch is missing",
        )

        assertTrue(
            parameterTypes.contains(listOf(String::class.java)),
            "single String constructor is missing",
        )
        assertTrue(
            parameterTypes.contains(listOf(String::class.java, String::class.java)),
            "two String constructor is missing",
        )
    }

    private fun assertBindsFromEnvironmentVariable(
        variableName: String,
        expected: TokenExchangeVersion,
    ) {
        contextRunner
            .withInitializer { context ->
                context.environment.propertySources.addFirst(
                    SystemEnvironmentPropertySource(
                        "systemEnvironment",
                        mapOf<String, Any>(variableName to "v2"),
                    ),
                )
            }.run { context ->
                assertEquals(
                    expected,
                    context.getBean(KeycloakConfig::class.java).tokenExchangeVersion,
                    "unexpected binding result for $variableName",
                )
            }
    }

    private fun bind(
        vararg properties: String,
        assertions: (KeycloakConfig) -> Unit,
    ) {
        contextRunner.withPropertyValues(*properties).run { context ->
            assertions(context.getBean(KeycloakConfig::class.java))
        }
    }

    @Configuration
    @EnableConfigurationProperties(KeycloakConfig::class)
    internal class TestConfiguration
}