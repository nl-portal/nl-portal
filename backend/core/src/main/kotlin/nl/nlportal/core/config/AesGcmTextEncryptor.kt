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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor
import org.springframework.security.crypto.encrypt.TextEncryptor
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

/**
 * AES-256-GCM TextEncryptor for Configuration Panel values; must stay byte-compatible with the
 * Configuration Panel's ConfigurationValueEncryptor: Base64(IV ‖ ciphertext ‖ tag).
 */
class AesGcmTextEncryptor(
    base64Key: String,
) : TextEncryptor {
    private val encryptor =
        AesGcmBytesEncryptor
            .withSecretKey(SecretKeySpec(decodeKey(base64Key), "AES"))
            .build()

    override fun encrypt(text: String): String = Base64.getEncoder().encodeToString(encryptor.encrypt(text.toByteArray(UTF_8)))

    override fun decrypt(encryptedText: String): String =
        try {
            String(encryptor.decrypt(Base64.getDecoder().decode(encryptedText)), UTF_8)
        } catch (e: Exception) {
            logger.error { DECRYPT_FAILED }
            throw IllegalStateException(DECRYPT_FAILED, e)
        }

    private fun decodeKey(base64Key: String): ByteArray {
        val key =
            try {
                Base64.getDecoder().decode(base64Key)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("CONFIGURATION_PANEL_ENCRYPTION_KEY must be Base64", e)
            }
        require(key.size == KEY_LENGTH_BYTES) {
            "CONFIGURATION_PANEL_ENCRYPTION_KEY must be a Base64-encoded $KEY_LENGTH_BYTES-byte (AES-256) key, got ${key.size} bytes"
        }
        return key
    }

    private companion object {
        private const val KEY_LENGTH_BYTES = 32
        private const val DECRYPT_FAILED =
            "Cannot decrypt Configuration Panel value: CONFIGURATION_PANEL_ENCRYPTION_KEY must equal the panel's CONFIG_ENCRYPTION_KEY"
        private val logger = KotlinLogging.logger {}
    }
}