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
package nl.nlportal.core.security

import org.mockito.Mockito
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

/**
 * Test application that runs the REAL OauthSecurityAutoConfiguration, unlike TestApplication,
 * which replaces it with a permit-all chain.
 *
 * No component scanning, so TestApplication's chain stays out; @TestComponent keeps TestApplication's
 * scan from picking this class up. Use it with @SpringBootTest(classes = [SecurityTestApplication::class]).
 */
@TestComponent
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class SecurityTestApplication {
    /**
     * Mock decoder for the main chain; never called for actuator requests.
     */
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder = Mockito.mock(ReactiveJwtDecoder::class.java)

    /**
     * Stand-in for the main chain's converter; in the app that is CommonGroundAuthenticationConverter.
     */
    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> = ReactiveJwtAuthenticationConverterAdapter(JwtAuthenticationConverter())

    /**
     * Stand-ins for the actuator endpoints: core has no actuator, the security chain is what is under test.
     */
    @Bean
    fun actuatorRoutes(): RouterFunction<ServerResponse> =
        router {
            GET("/actuator/health") { ServerResponse.ok().build() }
            GET("/actuator/health/liveness") { ServerResponse.ok().build() }
            POST("/actuator/refresh") { ServerResponse.ok().build() }
        }
}