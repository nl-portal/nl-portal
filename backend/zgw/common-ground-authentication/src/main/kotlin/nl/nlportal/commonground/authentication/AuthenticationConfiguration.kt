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

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.portal.authentication.service.PortalAuthenticationConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.core.io.ResourceLoader
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@EnableConfigurationProperties(KeycloakConfig::class, AuthenticationMachtigingsDienstConfig::class)
@AutoConfiguration
class AuthenticationConfiguration {
    @Bean
    fun authenticationMachtigingsDienstService(
        authenticationMachtigingsDienstConfig: AuthenticationMachtigingsDienstConfig,
        resourceLoader: ResourceLoader,
    ): AuthenticationMachtigingsDienstService =
        AuthenticationMachtigingsDienstService(
            authenticationMachtigingsDienstConfig,
            resourceLoader,
        )

    @Order(value = 0)
    @Bean
    fun commonGroundAuthenticationConverter(
        reactiveJwtDecoder: ReactiveJwtDecoder,
        keycloakConfig: KeycloakConfig,
    ): PortalAuthenticationConverter {
        logger.info { "Keycloak token exchange mode: ${keycloakConfig.tokenExchangeVersion}" }
        val audience = keycloakConfig.audience
        when (keycloakConfig.tokenExchangeVersion) {
            KeycloakConfig.TokenExchangeVersion.V1 ->
                if (audience.isNullOrBlank()) {
                    logger.warn {
                        "Token exchange is configured for V1 but nl-portal.authentication.keycloak.audience " +
                            "is not set. V1 requires an audience naming the target client, and every exchange " +
                            "will fail until it is configured."
                    }
                }

            KeycloakConfig.TokenExchangeVersion.V2 ->
                if (!audience.isNullOrBlank()) {
                    logger.info {
                        "V2 token exchange will request audience '$audience'. It must be an audience the " +
                            "client '${keycloakConfig.resource}' can already produce."
                    }
                }
        }
        return CommonGroundAuthenticationConverter(reactiveJwtDecoder, keycloakConfig)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}