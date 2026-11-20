/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.security.config.ActuatorSecurityConfig
import nl.nlportal.core.security.config.HttpSecurityConfigurer
import nl.nlportal.core.security.config.SecurityEndpointsConfig
import nl.nlportal.core.security.config.SecurityHeadersConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono
import java.time.Duration

@AutoConfiguration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(
    CorsPathConfiguration::class,
    SecurityHeadersConfig::class,
    SecurityEndpointsConfig::class,
    ActuatorSecurityConfig::class,
)
class OauthSecurityAutoConfiguration {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun actuatorSecurityWebFilterChain(
        http: ServerHttpSecurity,
        actuatorSecurityConfig: ActuatorSecurityConfig,
    ): SecurityWebFilterChain {
        val jwkSetUri = actuatorSecurityConfig.jwkSetUri?.takeIf { it.isNotBlank() }
        if (jwkSetUri == null) {
            logger.warn { "nl-portal.security.actuator.jwk-set-uri is not set: actuator endpoints other than health are denied" }
        }

        http
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/actuator/**"))
            .csrf { it.disable() }
            .authorizeExchange {
                it.pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                if (jwkSetUri == null) {
                    it.anyExchange().denyAll()
                } else {
                    it.anyExchange().hasAnyAuthority(*actuatorSecurityConfig.authorities.toTypedArray())
                }
            }

        jwkSetUri?.let { uri ->
            val authoritiesConverter =
                ExpressionJwtGrantedAuthoritiesConverter(
                    SpelExpressionParser().parseRaw(actuatorSecurityConfig.authoritiesClaimExpression),
                ).apply { setAuthorityPrefix("") }
            val authenticationConverter =
                JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authoritiesConverter) }
            val jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(uri).build()
            actuatorSecurityConfig.issuerUri?.takeIf { it.isNotBlank() }?.let { issuer ->
                jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer))
            }

            http.oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(authenticationConverter))
                }
            }
        }

        return http.build()
    }

    @Bean
    fun springSecurityWebFilterChain(
        http: ServerHttpSecurity,
        converter: Converter<Jwt, out Mono<out AbstractAuthenticationToken>>?,
        corsPathConfiguration: CorsPathConfiguration,
        securityConfigurers: List<HttpSecurityConfigurer>,
        securityHeadersConfig: SecurityHeadersConfig,
        securityEndpointsConfig: SecurityEndpointsConfig,
    ): SecurityWebFilterChain {
        securityConfigurers.forEach { it.configure(http) }

        return http
            .csrf { it.disable() }
            .headers { h ->
                h.frameOptions {
                    XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN
                }
                h.xssProtection(withDefaults())
                h.cache(withDefaults())
                securityHeadersConfig.contentSecurityPolicy?.let { spec ->
                    h.contentSecurityPolicy {
                        it.policyDirectives(spec)
                    }
                }
                h.hsts { c ->
                    c
                        .maxAge(Duration.ofDays(securityHeadersConfig.hstsMaxAgeInDays))
                        .includeSubdomains(true)
                        .preload(true)
                }
                h.contentTypeOptions(withDefaults())
                h.referrerPolicy { c ->
                    c.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.SAME_ORIGIN)
                }
            }.authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers("/graphiql").permitAll()
                it.pathMatchers("/graphql").permitAll()
                securityEndpointsConfig.unsecured.forEach { endpoint ->
                    it.pathMatchers(endpoint).permitAll()
                }
                it.anyExchange().authenticated()
            }.oauth2ResourceServer {
                it.jwt { t ->
                    t.jwtAuthenticationConverter(converter)
                }
            }.build()
    }

    @Bean
    fun corsWebFilter(corsPathConfiguration: CorsPathConfiguration): CorsWebFilter {
        val source = UrlBasedCorsConfigurationSource()

        corsPathConfiguration.cors.forEach {
            source.registerCorsConfiguration(it.path, it.config)
        }

        return CorsWebFilter(source)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}