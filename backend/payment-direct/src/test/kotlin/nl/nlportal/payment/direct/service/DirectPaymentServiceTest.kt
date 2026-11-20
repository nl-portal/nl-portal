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
package nl.nlportal.payment.direct.service

import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration.DirectPaymentProfile
import nl.nlportal.payment.direct.autoconfiguration.DirectPaymentModuleConfiguration.DirectPaymentProperties
import nl.nlportal.payment.direct.client.DirectPaymentClient
import nl.nlportal.payment.direct.domain.DirectPaymentException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class DirectPaymentServiceTest {
    private val properties =
        DirectPaymentProperties(
            configurations =
                mapOf(
                    "belastingzaken" to DirectPaymentProfile(pspId = "TAX"),
                    "anderemerchant" to DirectPaymentProfile(pspId = "OTHER"),
                ),
        )
    private val directPaymentService =
        DirectPaymentService(
            directPaymentModuleConfiguration = DirectPaymentModuleConfiguration(enabled = true, properties = properties),
            directPaymentClient = DirectPaymentClient(directPaymentProperties = properties),
        )

    @Test
    fun `toCents converts amounts exactly`() {
        assertEquals(29L, DirectPaymentService.toCents(0.29))
        assertEquals(10024L, DirectPaymentService.toCents(100.24))
        assertEquals(14743L, DirectPaymentService.toCents(147.43))
        assertEquals(1250L, DirectPaymentService.toCents(12.5))
        assertEquals(0L, DirectPaymentService.toCents(0.0))
    }

    @Test
    fun `toCents rejects amounts with more than 2 decimals`() {
        assertThrows<DirectPaymentException.AmountNotConvertible> { DirectPaymentService.toCents(10.005) }
    }

    @Test
    fun `verifyMerchant accepts the same profile by key or pspId`() {
        assertDoesNotThrow { directPaymentService.verifyMerchant("belastingzaken", "belastingzaken") }
        assertDoesNotThrow { directPaymentService.verifyMerchant("TAX", "belastingzaken") }
        assertDoesNotThrow { directPaymentService.verifyMerchant("belastingzaken", "TAX") }
    }

    @Test
    fun `verifyMerchant rejects another merchant`() {
        assertThrows<DirectPaymentException.MerchantMismatch> { directPaymentService.verifyMerchant("anderemerchant", "TAX") }
    }

    @Test
    fun `verifyMerchant rejects unknown profiles`() {
        assertThrows<DirectPaymentException.MerchantMismatch> { directPaymentService.verifyMerchant("onbekend", "TAX") }
        assertThrows<DirectPaymentException.MerchantMismatch> { directPaymentService.verifyMerchant("onbekend", "onbekend") }
    }
}