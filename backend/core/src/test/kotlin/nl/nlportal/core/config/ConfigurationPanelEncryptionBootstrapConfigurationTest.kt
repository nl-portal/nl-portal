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

import nl.nlportal.core.config.AesGcmTextEncryptorTest.Companion.VECTOR_CIPHERTEXT
import nl.nlportal.core.config.AesGcmTextEncryptorTest.Companion.VECTOR_KEY
import nl.nlportal.core.config.AesGcmTextEncryptorTest.Companion.VECTOR_PLAINTEXT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration

/**
 * Runs a real Spring Cloud bootstrap: the config client is enabled but points nowhere (fail-fast off),
 * so the {cipher} value comes from the command line instead of a config server.
 */
internal class ConfigurationPanelEncryptionBootstrapConfigurationTest {
    @Test
    fun `decrypts cipher values when the Configuration Panel and encryption are enabled`() {
        run(CONFIG_ENABLED, "--nl-portal.configuration-panel.encryption-key=$VECTOR_KEY", CIPHER_VALUE).use { context ->
            assertEquals(VECTOR_PLAINTEXT, context.environment.getProperty("test.value"))
            assertTrue(context.containsBean(BEAN_NAME))
        }
    }

    @Test
    fun `no encryptor when encryption is disabled`() {
        run(CONFIG_ENABLED, ENCRYPTION_DISABLED).use { context ->
            assertFalse(context.containsBean(BEAN_NAME))
        }
    }

    @Test
    fun `a cipher value fails startup when encryption is disabled`() {
        val exception = assertThrows<Exception> { run(CONFIG_ENABLED, ENCRYPTION_DISABLED, CIPHER_VALUE).close() }

        assertTrue(causes(exception).any { it.message?.contains("Cannot decrypt: key=test.value") == true })
    }

    @Test
    fun `no encryptor and no key needed when the Configuration Panel is disabled`() {
        run("--spring.cloud.config.enabled=false").use { context ->
            assertFalse(context.containsBean(BEAN_NAME))
        }
    }

    @Test
    fun `startup fails when encryption is enabled without a key`() {
        val exception = assertThrows<Exception> { run(CONFIG_ENABLED).close() }

        assertTrue(causes(exception).any { it.message?.contains("CONFIGURATION_PANEL_ENCRYPTION_KEY is required") == true })
    }

    @Test
    fun `startup fails when Spring Cloud's own encrypt key is also set`() {
        val exception =
            assertThrows<Exception> {
                run(CONFIG_ENABLED, "--nl-portal.configuration-panel.encryption-key=$VECTOR_KEY", "--encrypt.key=other").close()
            }

        assertTrue(causes(exception).any { it.message?.contains("cannot be combined with Configuration Panel encryption") == true })
    }

    private fun run(vararg args: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(EmptyConfiguration::class.java)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.cloud.config.uri=http://localhost:1",
                "--spring.cloud.config.fail-fast=false",
                "--spring.cloud.config.name=test",
                "--spring.cloud.config.token=test",
                *args,
            )

    private fun causes(exception: Throwable): Sequence<Throwable> = generateSequence(exception) { it.cause }

    @Configuration(proxyBeanMethods = false)
    internal class EmptyConfiguration

    companion object {
        private const val BEAN_NAME = "configurationPanelTextEncryptor"
        private const val CONFIG_ENABLED = "--spring.cloud.config.enabled=true"
        private const val ENCRYPTION_DISABLED = "--nl-portal.configuration-panel.encryption-enabled=false"
        private const val CIPHER_VALUE = "--test.value={cipher}$VECTOR_CIPHERTEXT"
    }
}