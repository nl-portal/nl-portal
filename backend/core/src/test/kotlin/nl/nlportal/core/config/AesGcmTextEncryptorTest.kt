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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AesGcmTextEncryptorTest {
    @Test
    fun `round trip`() {
        val encryptor = AesGcmTextEncryptor(VECTOR_KEY)

        val encrypted = encryptor.encrypt("some value")

        assertNotEquals("some value", encrypted)
        assertEquals("some value", encryptor.decrypt(encrypted))
    }

    @Test
    fun `the cross-repo vector decrypts to the expected plaintext`() {
        assertEquals(VECTOR_PLAINTEXT, AesGcmTextEncryptor(VECTOR_KEY).decrypt(VECTOR_CIPHERTEXT))
    }

    @Test
    fun `a wrong key fails with the key mismatch message`() {
        val exception =
            assertThrows<IllegalStateException> {
                AesGcmTextEncryptor(OTHER_KEY).decrypt(VECTOR_CIPHERTEXT)
            }

        assertTrue(exception.message!!.contains("CONFIGURATION_PANEL_ENCRYPTION_KEY must equal the panel's CONFIG_ENCRYPTION_KEY"))
    }

    @Test
    fun `a key that is not 32 bytes is rejected`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                AesGcmTextEncryptor(SHORT_KEY)
            }

        assertTrue(exception.message!!.contains("32-byte (AES-256) key, got 16 bytes"))
    }

    @Test
    fun `a key that is not Base64 is rejected`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                AesGcmTextEncryptor("not base64!")
            }

        assertTrue(exception.message!!.contains("CONFIGURATION_PANEL_ENCRYPTION_KEY must be Base64"))
    }

    companion object {
        // Shared with the Configuration Panel's ConfigurationValueEncryptorTest; generated once with plain JDK AES/GCM/NoPadding.
        const val VECTOR_KEY = "u8nzUYr3cgDzLfwQE+IWU73QbPFORBBG+hjk1XNiXuI="
        const val VECTOR_CIPHERTEXT =
            "bHvURiubAwRK0j+Ra5eSUSRSwlXHeXJSC8/uH/V9hw84qX+k1GhmuiZ9EJ7KryG+dAYXESNCHyWTov4mNEqcxlMESjRFVvE0UjX16D6AEw=="
        const val VECTOR_PLAINTEXT = "Configuration Panel cross-repo vector: €ü✓"

        private const val OTHER_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        private const val SHORT_KEY = "AAAAAAAAAAAAAAAAAAAAAA=="
    }
}