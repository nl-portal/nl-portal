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

import nl.nlportal.portal.authentication.service.PortalAuthenticationConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

internal class AuthenticationConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthenticationConfiguration::class.java))
            .withUserConfiguration(JwtDecoderConfiguration::class.java)

    @Test
    fun `a v1 configuration without an audience fails to start`() {
        contextRunner
            .withPropertyValues(
                "nl-portal.authentication.keycloak.token-exchange-version=v1",
                "nl-portal.authentication.keycloak.resource=nl-portal-m2m",
                "nl-portal.authentication.keycloak.audience=",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage(CommonGroundAuthenticationConverter.AUDIENCE_REQUIRED_MESSAGE)
            }
    }

    @Test
    fun `a v1 configuration with an audience starts`() {
        contextRunner
            .withPropertyValues(
                "nl-portal.authentication.keycloak.token-exchange-version=v1",
                "nl-portal.authentication.keycloak.resource=nl-portal-m2m",
                "nl-portal.authentication.keycloak.audience=nl-portal-token-exchange",
            ).run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(PortalAuthenticationConverter::class.java)
            }
    }

    @Test
    fun `a context without a configured keycloak client starts, whatever the version`() {
        contextRunner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(PortalAuthenticationConverter::class.java)
        }
    }

    @Test
    fun `a v2 configuration without an audience starts`() {
        contextRunner
            .withPropertyValues(
                "nl-portal.authentication.keycloak.token-exchange-version=v2",
                "nl-portal.authentication.keycloak.resource=nl-portal-m2m",
                "nl-portal.authentication.keycloak.audience=",
            ).run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(PortalAuthenticationConverter::class.java)
            }
    }

    @Configuration
    internal class JwtDecoderConfiguration {
        @Bean
        fun reactiveJwtDecoder(): ReactiveJwtDecoder = mock()
    }
}