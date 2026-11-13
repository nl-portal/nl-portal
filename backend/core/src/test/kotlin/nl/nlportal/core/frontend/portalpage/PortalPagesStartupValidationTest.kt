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
package nl.nlportal.core.frontend.portalpage

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageProperties
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class PortalPagesStartupValidationTest {
    @Test
    fun `an unexpected exception while checking is one error, never a startup failure`(output: CapturedOutput) {
        val throwing =
            object : PortalPageBindingResolver {
                override val type = "throws"
                override val fixedParams: Set<String> get() = throw IllegalStateException("boom")

                override suspend fun resolve(
                    context: PortalPageRequestContext,
                    params: PortalPageParams,
                ) = throw UnsupportedOperationException()
            }
        val properties =
            FrontendConfigurationProperties().apply {
                pages = mapOf("p" to PortalPageProperties(type = "dynamic", bindings = mapOf("x" to PortalPageBindingProperties(resolver = "throws"))))
            }

        PortalPagesStartupValidation(properties, PortalPageConfigValidator(listOf(throwing), PortalPageProjection())).validate()

        assertTrue(output.all.lines().any { it.contains("ERROR") && it.contains("Checking nl-portal.frontend.pages failed") }, output.all)
    }
}