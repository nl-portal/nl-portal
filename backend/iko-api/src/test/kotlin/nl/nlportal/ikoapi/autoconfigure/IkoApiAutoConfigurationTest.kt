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
package nl.nlportal.ikoapi.autoconfigure

import nl.nlportal.ikoapi.client.IkoApiClient
import nl.nlportal.ikoapi.client.IkoApiConfig.IkoApiConfigProperties
import nl.nlportal.ikoapi.service.IkoApiService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.reactive.function.client.WebClient
import java.util.function.Supplier

class IkoApiAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IkoApiAutoConfiguration::class.java))
            .withBean(WebClient.Builder::class.java, Supplier { WebClient.builder() })

    private val requiredProperties =
        mapOf(
            "url" to "http://localhost:8084",
            "token-url" to "http://localhost:8082/auth/realms/iko/protocol/openid-connect/token",
            "client-id" to "nl-portal",
            "client-secret" to "secret",
        )

    @Test
    fun `creates no beans when enabled is absent`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(IkoApiService::class.java)
            assertThat(context).doesNotHaveBean(IkoApiClient::class.java)
        }
    }

    @Test
    fun `creates no beans when disabled`() {
        contextRunner
            .withPropertyValues("nl-portal.config.ikoapi.enabled=false", *properties(requiredProperties))
            .run { context ->
                assertThat(context).doesNotHaveBean(IkoApiService::class.java)
                assertThat(context).doesNotHaveBean(IkoApiClient::class.java)
            }
    }

    @Test
    fun `creates client and service when enabled with all required properties`() {
        contextRunner
            .withPropertyValues("nl-portal.config.ikoapi.enabled=true", *properties(requiredProperties))
            .run { context ->
                assertThat(context).hasSingleBean(IkoApiService::class.java)
                assertThat(context).hasSingleBean(IkoApiClient::class.java)
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["url", "token-url", "client-id", "client-secret"])
    fun `fails when a required property is missing`(missing: String) {
        contextRunner
            .withPropertyValues("nl-portal.config.ikoapi.enabled=true", *properties(requiredProperties - missing))
            .run { context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessage("nl-portal.config.ikoapi.properties.$missing is required")
            }
    }

    @Test
    fun `keeps a user-defined IkoApiService`() {
        val custom = IkoApiService(IkoApiClient(WebClient.create()), IkoApiConfigProperties())

        contextRunner
            .withPropertyValues("nl-portal.config.ikoapi.enabled=true", *properties(requiredProperties))
            .withBean("customIkoApiService", IkoApiService::class.java, Supplier { custom })
            .run { context ->
                assertThat(context).hasSingleBean(IkoApiService::class.java)
                assertThat(context.getBean(IkoApiService::class.java)).isSameAs(custom)
            }
    }

    private fun properties(values: Map<String, String>): Array<String> = values.map { (key, value) -> "nl-portal.config.ikoapi.properties.$key=$value" }.toTypedArray()
}