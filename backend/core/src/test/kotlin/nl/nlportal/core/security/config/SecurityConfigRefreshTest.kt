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

import nl.nlportal.core.security.CorsPathConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder
import org.springframework.context.annotation.Configuration

internal class SecurityConfigRefreshTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    TestConfiguration::class.java,
                    ConfigurationPropertiesRebinderAutoConfiguration::class.java,
                ),
            )

    @Test
    fun `a configuration refresh keeps the bound security endpoints and cors paths`() {
        contextRunner
            .withPropertyValues(
                "nl-portal.security.endpoints.unsecured[0]=/actuator/health",
                "nl-portal.security.cors[0].path=/**",
                "nl-portal.security.cors[0].config.allowed-origins[0]=*",
            ).run { context ->
                context.getBean(ConfigurationPropertiesRebinder::class.java).rebind()

                assertEquals(
                    listOf("/actuator/health"),
                    context.getBean(SecurityEndpointsConfig::class.java).unsecured,
                )
                val cors = context.getBean(CorsPathConfiguration::class.java).cors
                assertEquals(1, cors.size)
                assertEquals("/**", cors[0].path)
                assertEquals(listOf("*"), cors[0].config.allowedOrigins)
            }
    }

    @Configuration
    @EnableConfigurationProperties(SecurityEndpointsConfig::class, CorsPathConfiguration::class)
    internal class TestConfiguration
}