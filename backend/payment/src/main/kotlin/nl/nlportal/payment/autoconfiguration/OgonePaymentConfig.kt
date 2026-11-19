/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
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
package nl.nlportal.payment.autoconfiguration

import nl.nlportal.core.util.ShaVersion
import org.springframework.boot.context.properties.ConfigurationProperties

@Deprecated("Ogone e-Commerce is no longer supported and will be removed in 4.0.0. Use the payment-direct module instead.")
@ConfigurationProperties(prefix = "nl-portal.config.payment.ogone", ignoreUnknownFields = true)
class OgonePaymentConfig {
    var enabled: Boolean = true
    var properties: OgonePaymentConfigProperties = OgonePaymentConfigProperties()

    class OgonePaymentConfigProperties {
        var url: String = ""
        var shaOutParameters: List<String> = emptyList()
        var configurations: Map<String, OgonePaymentProfile> = mapOf()

        fun getPaymentProfile(profileIdentifier: String): OgonePaymentProfile? = configurations[profileIdentifier]

        fun getPaymentProfileByPspPid(pspId: String?): OgonePaymentProfile? {
            configurations.forEach {
                if (it.value.pspId == pspId) {
                    return it.value
                }
            }
            return null
        }

        /**
         * Finds a payment profile by its key or, when no profile has that key, by its pspId.
         */
        fun findPaymentProfile(identifier: String?): OgonePaymentProfile? = identifier?.let { getPaymentProfile(it) ?: getPaymentProfileByPspPid(it) }
    }

    class OgonePaymentProfile {
        var pspId: String = ""
        var language: String = "nl_NL"
        var currency: String = "EUR"
        var title: String = ""
        var shaInKey: String = ""
        var shaOutKey: String = ""
        var shaVersion: String = ShaVersion.SHA1.version
        var failureUrl: String = ""
        var successUrl: String = ""
    }
}