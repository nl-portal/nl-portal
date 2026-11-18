/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.util

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.web.reactive.function.client.WebClientResponseException

object CoreUtils {
    @JvmStatic
    fun extractId(url: String): UUID = UUID.fromString(url.substringAfterLast("/"))

    @JvmStatic
    @Throws(NoSuchAlgorithmException::class)
    fun createHash(
        input: String,
        shaVersion: String,
    ): String =
        when (shaVersion) {
            ShaVersion.SHA256.version -> {
                DigestUtils.sha512Hex(input)
            }

            ShaVersion.SHA512.version -> {
                DigestUtils.sha512Hex(input)
            }

            else -> {
                DigestUtils.sha1Hex(input)
            }
        }

    /**
     * A short description of [e] for a log line: its type, plus the status of a failed WebClient call.
     * Never the message: for WebClient calls it holds the full request URL, identifiers such as a BSN in the query string included.
     * Log the exception itself at trace level for debugging.
     */
    @JvmStatic
    fun logSummary(e: Throwable): String =
        when (e) {
            is WebClientResponseException -> "${e.javaClass.simpleName} ${e.statusCode.value()}"
            else -> e.javaClass.simpleName
        }

    @JvmStatic
    fun generateNonce(byteLength: Int = 16): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }
}