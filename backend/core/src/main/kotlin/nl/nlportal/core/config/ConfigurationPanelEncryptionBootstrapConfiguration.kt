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
package nl.nlportal.core.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.crypto.encrypt.TextEncryptor

/**
 * Registered in the Spring Cloud bootstrap context (META-INF/spring.factories), where
 * EncryptionBootstrapConfiguration picks up the TextEncryptor to decrypt {cipher} values
 * served by the Configuration Panel, at startup and on refresh.
 *
 * Ordered first so Spring Cloud's own TextEncryptor (@ConditionalOnMissingBean) backs off; combining
 * it with encrypt.* fails startup instead of silently picking one.
 */
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnBooleanProperty("spring.cloud.config.enabled")
@ConditionalOnBooleanProperty("nl-portal.configuration-panel.encryption-enabled", matchIfMissing = true)
@EnableConfigurationProperties(ConfigurationPanelEncryptionConfig::class)
class ConfigurationPanelEncryptionBootstrapConfiguration {
    @Bean
    fun configurationPanelTextEncryptor(
        config: ConfigurationPanelEncryptionConfig,
        environment: Environment,
    ): TextEncryptor {
        val key = config.encryptionKey
        check(SPRING_CLOUD_KEY_PROPERTIES.none { !environment.getProperty(it).isNullOrBlank() }) {
            "encrypt.key and encrypt.key-store cannot be combined with Configuration Panel encryption; " +
                "set CONFIGURATION_PANEL_ENCRYPTION_ENABLED=false to use Spring Cloud's own encryption"
        }
        require(!key.isNullOrBlank()) {
            "CONFIGURATION_PANEL_ENCRYPTION_KEY is required when the Configuration Panel is enabled; " +
                "set CONFIGURATION_PANEL_ENCRYPTION_ENABLED=false to opt out"
        }
        return AesGcmTextEncryptor(key)
    }

    private companion object {
        private val SPRING_CLOUD_KEY_PROPERTIES = listOf("encrypt.key", "encrypt.key-store.location")
    }
}