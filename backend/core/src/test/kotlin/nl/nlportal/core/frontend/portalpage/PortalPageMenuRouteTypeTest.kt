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
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuRouteProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageTemplateProperties
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PortalPageMenuRouteTypeTest {
    private val template = PortalPageTemplateProperties(source = "fake", params = mapOf("name" to "x"))
    private val properties =
        FrontendConfigurationProperties().apply {
            pages =
                mapOf(
                    "over-ons" to PortalPageProperties(type = "static", template = template),
                    "kapot" to PortalPageProperties(type = "static"),
                    "half" to
                        PortalPageProperties(
                            type = "dynamic",
                            template = template,
                            bindings = mapOf("x" to PortalPageBindingProperties(resolver = "onbekend")),
                        ),
                )
        }
    private val routeType =
        PortalPageMenuRouteType(
            properties,
            PortalPageConfigValidator(
                listOf(FakePortalPageBindingResolver()),
                PortalPageProjection(),
                listOf(FakePortalPageTemplateSource(templates = mapOf("x" to PortalPageTemplate("x")))),
            ),
        )

    @Test
    fun `a valid page is accepted, also when its key differs in case and dashes`() {
        assertEquals(emptyList<String>(), routeType.problems(entry("over-ons")))
        assertEquals(emptyList<String>(), routeType.problems(entry("OVERONS")))
    }

    @Test
    fun `a missing page is a problem`() {
        assertEquals(listOf("page 'onbekend' is not configured in pages"), routeType.problems(entry("onbekend")))
    }

    @Test
    fun `an invalid page is a problem that only points at the page`() {
        assertEquals(listOf("page 'kapot' is invalid, see pages.kapot"), routeType.problems(entry("kapot")))
    }

    @Test
    fun `two pages equal ignoring case and dashes are invalid`() {
        val clash =
            FrontendConfigurationProperties().apply {
                pages =
                    mapOf(
                        "over-ons" to PortalPageProperties(type = "static", template = template),
                        "OverOns" to PortalPageProperties(type = "static", template = template),
                    )
            }
        val routeType =
            PortalPageMenuRouteType(
                clash,
                PortalPageConfigValidator(emptyList(), PortalPageProjection(), listOf(FakePortalPageTemplateSource(templates = mapOf("x" to PortalPageTemplate("x"))))),
            )

        assertEquals(listOf("page 'over-ons' is invalid, see pages.over-ons"), routeType.problems(entry("over-ons")))
    }

    @Test
    fun `binding problems don't count`() {
        assertEquals(emptyList<String>(), routeType.problems(entry("half")))
    }

    @Test
    fun `a sub path is a problem`() {
        assertEquals(listOf("route.path may not end in /* for route.type portal-page"), routeType.problems(entry("over-ons", path = "/over-ons/*")))
    }

    private fun entry(
        page: String,
        path: String = "/p",
    ) = FrontendMenuEntryProperties(name = "p", route = FrontendMenuRouteProperties(type = "portal-page", page = page, path = path))
}