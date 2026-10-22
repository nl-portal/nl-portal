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
package nl.nlportal.core.frontend.menu

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuRouteProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.TestingAuthenticationToken

internal class FrontendMenuServiceTest {
    private val authentication = TestingAuthenticationToken("user", null)

    @Test
    fun `invalid configuration fails with one message listing every violation`() {
        val properties =
            properties(
                entry(name = "Bad Name", type = "default", page = "", path = "no-slash", group = -1, ordering = -1),
                entry(name = "dup", type = "", path = "/dup"),
                entry(name = "dup", path = "/dup"),
                entry(name = "params", path = "/extern/:id"),
                entry(name = "splat", path = "/extern/*"),
            )

        val exception = assertThrows<IllegalStateException> { FrontendMenuService(properties, null) }
        val message = exception.message!!

        listOf(
            "menu-entries[0] (Bad Name): name must match",
            "menu-entries[0] (Bad Name): route.path must start with /",
            "menu-entries[0] (Bad Name): route.type must be one of templated, custom, was 'default'",
            "menu-entries[0] (Bad Name): route.page must not be blank",
            "menu-entries[0] (Bad Name): group must be >= 0",
            "menu-entries[0] (Bad Name): ordering must be >= 0",
            "menu-entries[1] (dup): route.type must be one of templated, custom, was ''",
            "menu-entries[1] (dup): name must be unique",
            "menu-entries[1] (dup): route.path must be unique",
            "menu-entries[2] (dup): name must be unique",
            "menu-entries[2] (dup): route.path must be unique",
            "menu-entries[3] (params): route.path must not contain : or *",
            "menu-entries[4] (splat): route.path must not contain : or *",
        ).forEach { assertTrue(message.contains(it), "Expected '$it' in:\n$message") }
    }

    @Test
    fun `valid configuration starts`() {
        FrontendMenuService(properties(entry(name = "a", path = "/a"), entry(name = "b", type = "custom", path = "/b")), null)
    }

    @Test
    fun `entry without audience is visible to every audience`() {
        val properties = properties(entry(name = "all", path = "/all"))

        listOf("burger", "bedrijf", FrontendMenuAudienceResolver.SUBJECT).forEach { audience ->
            assertEquals(listOf("all"), names(service(properties, audience)))
        }
    }

    @Test
    fun `burger entry is hidden for bedrijf and subject`() {
        val properties = properties(entry(name = "burger-only", path = "/burger", audience = listOf("burger")))

        assertEquals(listOf("burger-only"), names(service(properties, "burger")))
        assertEquals(emptyList<String>(), names(service(properties, "bedrijf")))
        assertEquals(emptyList<String>(), names(service(properties, FrontendMenuAudienceResolver.SUBJECT)))
    }

    @Test
    fun `subject entry is visible without a resolver`() {
        val properties = properties(entry(name = "subject-only", path = "/subject", audience = listOf("subject")))

        assertEquals(listOf("subject-only"), names(FrontendMenuService(properties, null)))
    }

    @Test
    fun `audience match is case-insensitive`() {
        val properties = properties(entry(name = "bedrijf-only", path = "/bedrijf", audience = listOf("Bedrijf")))

        assertEquals(listOf("bedrijf-only"), names(service(properties, "BEDRIJF")))
    }

    @Test
    fun `config order is preserved and fields are copied`() {
        val properties =
            properties(
                entry(name = "c", path = "/c", group = 2, ordering = 1),
                entry(name = "a", type = "custom", path = "/a"),
                entry(name = "b", path = "/b"),
            )

        val entries = FrontendMenuService(properties, null).getMenuEntries(authentication)

        assertEquals(listOf("c", "a", "b"), entries.map { it.name })
        assertEquals(2, entries[0].group)
        assertEquals(1, entries[0].ordering)
        assertEquals("custom", entries[1].route.type)
        assertEquals("/a", entries[1].route.path)
        assertEquals("page", entries[1].route.page)
        assertEquals("DocumentIcon", entries[1].icon)
    }

    private fun service(
        properties: FrontendConfigurationProperties,
        audience: String,
    ) = FrontendMenuService(properties) { audience }

    private fun names(service: FrontendMenuService) = service.getMenuEntries(authentication).map { it.name }

    private fun properties(vararg entries: FrontendMenuEntryProperties) = FrontendConfigurationProperties().apply { menuEntries = entries.toList() }

    private fun entry(
        name: String,
        path: String,
        type: String = "templated",
        page: String = "page",
        group: Int? = null,
        ordering: Int? = null,
        audience: List<String> = emptyList(),
    ) = FrontendMenuEntryProperties(
        name = name,
        group = group,
        ordering = ordering,
        audience = audience,
        route = FrontendMenuRouteProperties(type = type, page = page, path = path),
    )
}