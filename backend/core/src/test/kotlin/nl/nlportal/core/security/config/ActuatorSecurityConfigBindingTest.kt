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
package nl.nlportal.core.security.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.SystemEnvironmentPropertySource

internal class ActuatorSecurityConfigBindingTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration::class.java))

    @Test
    fun `defaults apply when nothing is set`() {
        contextRunner.run { context ->
            val config = context.getBean(ActuatorSecurityConfig::class.java)
            assertNull(config.jwkSetUri)
            assertNull(config.issuerUri)
            assertEquals("['realm_access']['roles']", config.authoritiesClaimExpression)
            assertEquals(listOf("ROLE_ACTUATOR"), config.authorities)
        }
    }

    @Test
    fun `the canonical environment variables bind`() {
        contextRunner
            .withInitializer { context ->
                context.environment.propertySources.addFirst(
                    SystemEnvironmentPropertySource(
                        "systemEnvironment",
                        mapOf<String, Any>(
                            "NLPORTAL_SECURITY_ACTUATOR_JWKSETURI" to "http://keycloak/realms/gzac/protocol/openid-connect/certs",
                            "NLPORTAL_SECURITY_ACTUATOR_ISSUERURI" to "http://keycloak/realms/gzac",
                            "NLPORTAL_SECURITY_ACTUATOR_AUTHORITIESCLAIMEXPRESSION" to "['resource_access']['nl-portal']['roles']",
                            "NLPORTAL_SECURITY_ACTUATOR_AUTHORITIES" to "ROLE_A,ROLE_B",
                        ),
                    ),
                )
            }.run { context ->
                val config = context.getBean(ActuatorSecurityConfig::class.java)
                assertEquals("http://keycloak/realms/gzac/protocol/openid-connect/certs", config.jwkSetUri)
                assertEquals("http://keycloak/realms/gzac", config.issuerUri)
                assertEquals("['resource_access']['nl-portal']['roles']", config.authoritiesClaimExpression)
                assertEquals(listOf("ROLE_A", "ROLE_B"), config.authorities)
            }
    }

    @Test
    fun `a configuration refresh keeps the bound values`() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesRebinderAutoConfiguration::class.java))
            .withPropertyValues(
                "nl-portal.security.actuator.jwk-set-uri=http://keycloak/certs",
                "nl-portal.security.actuator.authorities=ROLE_A",
            ).run { context ->
                context.getBean(ConfigurationPropertiesRebinder::class.java).rebind()

                val config = context.getBean(ActuatorSecurityConfig::class.java)
                assertEquals("http://keycloak/certs", config.jwkSetUri)
                assertEquals(listOf("ROLE_A"), config.authorities)
            }
    }

    @Configuration
    @EnableConfigurationProperties(ActuatorSecurityConfig::class)
    internal class TestConfiguration
}