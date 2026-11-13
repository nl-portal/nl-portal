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

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.configuration.FrontendBindFailures
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuRouteProperties
import nl.nlportal.core.frontend.menu.service.FrontendMenuConditionalCheckers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication

@ExtendWith(OutputCaptureExtension::class)
internal class FrontendMenuServiceTest {
    private val authentication = TestingAuthenticationToken("user", null)
    private val noConditions = FrontendMenuConditionalCheckers(emptyList())
    private val portalPage = routeType("portal-page")
    private val custom = routeType("custom")

    @Test
    fun `invalid configuration never fails construction and lists every problem per entry`() {
        val properties =
            properties(
                entry(name = "Bad Name", type = "default", page = "", path = "no-slash", group = -1, ordering = -1),
                entry(name = "dup", type = "", path = "/dup"),
                entry(name = "dup", path = "/dup"),
                entry(name = "params", path = "/extern/:id"),
                entry(name = "segments", type = "custom", path = "/a b/*/c"),
                entry(name = "bad-param", path = "/x/:1d", hidden = true),
                entry(name = "valid", path = "/valid"),
            )

        val problems = FrontendMenuService(properties, null, noConditions, listOf(portalPage, custom)).problems()

        assertEquals(
            mapOf(
                0 to
                    listOf(
                        "name must match ^[a-z0-9-]+$",
                        "route.path must start with /",
                        "route.page must not be blank",
                        "group must be >= 0",
                        "ordering must be >= 0",
                        "route.type 'default' is not available (unknown, or its feature is disabled)",
                    ),
                1 to
                    listOf(
                        "name must be unique",
                        "route.path must be unique",
                        "route.type '' is not available (unknown, or its feature is disabled)",
                    ),
                2 to listOf("name must be unique", "route.path must be unique"),
                3 to listOf("route.path with a :param requires hidden: true"),
                4 to listOf("route.path segments must be a literal [A-Za-z0-9._~-]+, a :param or a final *"),
                5 to listOf("route.path segments must be a literal [A-Za-z0-9._~-]+, a :param or a final *"),
            ),
            problems,
        )
    }

    @Test
    fun `entries with problems are left out and the rest is served`() =
        runTest {
            val properties =
                properties(
                    entry(name = "a", path = "/same"),
                    entry(name = "b", path = "/same"),
                    entry(name = "c", path = "/c"),
                    entry(name = "c", path = "/c2"),
                    entry(name = "ok", path = "/ok"),
                    entry(name = "params", path = "/extern/:id"),
                )

            assertEquals(listOf("ok"), names(FrontendMenuService(properties, null, noConditions, listOf(portalPage))))
        }

    @Test
    fun `entries of a route type without a bean are left out`() =
        runTest {
            val properties = properties(entry(name = "page", path = "/page"), entry(name = "extern", type = "custom", path = "/extern/*"))

            assertEquals(listOf("page"), names(FrontendMenuService(properties, null, noConditions, listOf(portalPage))))
        }

    @Test
    fun `problems of the route type leave the entry out`() =
        runTest {
            val picky = routeType("portal-page") { if (it.route.page == "kapot") listOf("page 'kapot' is invalid") else emptyList() }
            val throwing = routeType("custom") { throw IllegalStateException("boom") }
            val properties =
                properties(
                    entry(name = "good", path = "/good"),
                    entry(name = "bad", path = "/bad", page = "kapot"),
                    entry(name = "throws", type = "custom", path = "/throws/*"),
                )
            val service = FrontendMenuService(properties, null, noConditions, listOf(picky, throwing))

            assertEquals(listOf("good"), names(service))
            assertEquals(listOf("page 'kapot' is invalid"), service.problems()[1])
            assertEquals(listOf("route.type 'custom' check failed: boom"), service.problems()[2])
        }

    @Test
    fun `first route type wins for a duplicate type`() =
        runTest {
            val first = routeType("portal-page")
            val second = routeType("portal-page") { listOf("never used") }
            val properties = properties(entry(name = "a", path = "/a"))

            assertEquals(listOf("a"), names(FrontendMenuService(properties, null, noConditions, listOf(first, second))))
        }

    @Test
    fun `values Spring couldn't read leave the entry out`() =
        runTest {
            val bindFailures = FrontendBindFailures()
            bindFailures.record("nl-portal.frontend.menu-entries[1].ordering", "Failed to convert 'first'")
            val properties = properties(entry(name = "a", path = "/a"), entry(name = "b", path = "/b"))
            val service = FrontendMenuService(properties, null, noConditions, listOf(portalPage), bindFailures)

            assertEquals(listOf("a"), names(service))
            assertEquals(mapOf(1 to listOf("could not read menu-entries[1].ordering: Failed to convert 'first'")), service.problems())
        }

    @Test
    fun `problems are logged as errors, duplicate keys as warnings`(output: CapturedOutput) {
        val checkers = FrontendMenuConditionalCheckers(listOf(checker("a", "yes"), checker("a", "no")))
        val properties =
            properties(
                entry(name = "ok", path = "/ok"),
                entry(name = "extern", type = "custom", path = "/extern/*"),
                entry(name = "cond", path = "/cond", conditional = mapOf("onbekend" to "x")),
            )

        FrontendMenuService(properties, null, checkers, listOf(portalPage, portalPage)).logProblems()

        assertTrue(
            output.all.lines().any {
                it.contains("ERROR") &&
                    it.contains("Invalid nl-portal.frontend.menu-entries[1] (extern): route.type 'custom' is not available (unknown, or its feature is disabled); the entry is left out")
            },
            output.all,
        )
        assertTrue(output.all.contains("Invalid nl-portal.frontend.menu-entries[2] (cond): unknown condition 'onbekend'; the entry is left out"), output.all)
        assertTrue(output.all.contains("More than one menu condition checker for key 'a'; the first one is used"), output.all)
        assertTrue(output.all.contains("More than one menu route type 'portal-page'; the first one is used"), output.all)
        assertFalse(output.all.contains("menu-entries[0]"), output.all)
    }

    @Test
    fun `a refreshed configuration is logged again, requests log nothing`(output: CapturedOutput) =
        runTest {
            val properties = properties(entry(name = "a", path = "/a"))
            val service = FrontendMenuService(properties, null, noConditions, listOf(portalPage))

            service.logProblems()
            properties.menuEntries = listOf(entry(name = "a", path = "/a"), entry(name = "b", path = "/a"))
            service.getMenuEntries(authentication)
            assertFalse(output.all.contains("route.path must be unique"), output.all)

            service.logProblems()
            assertTrue(output.all.contains("Invalid nl-portal.frontend.menu-entries[1] (b): route.path must be unique"), output.all)
        }

    @Test
    fun `full router paths are accepted`() {
        val problems =
            FrontendMenuService(
                properties(
                    entry(name = "a", path = "/a.b_c~d-e"),
                    entry(name = "b", path = "/b/:id", hidden = true),
                    entry(name = "c", path = "/c/:caseId/details", hidden = true),
                    entry(name = "d", type = "custom", path = "/d/*"),
                ),
                null,
                noConditions,
                listOf(portalPage, custom),
            ).problems()

        assertEquals(emptyMap<Int, List<String>>(), problems)
    }

    @Test
    fun `hidden is passed through`() =
        runTest {
            val entries =
                FrontendMenuService(properties(entry(name = "a", path = "/a"), entry(name = "b", path = "/b/:id", hidden = true)), null, noConditions, listOf(portalPage))
                    .getMenuEntries(authentication)

            assertEquals(listOf(false, true), entries.map { it.hidden })
        }

    @Test
    fun `portal page is visible through hidden entries and respects audience`() =
        runTest {
            val properties =
                properties(
                    entry(name = "list", path = "/list", page = "Erfpacht", audience = listOf("burger")),
                    entry(name = "detail", path = "/list/:id", page = "product-detail", hidden = true),
                    entry(name = "extern", type = "custom", path = "/extern/*", page = "extern"),
                )

            assertTrue(service(properties, "burger").isPortalPageVisible(authentication, "erfpacht"))
            assertFalse(service(properties, "bedrijf").isPortalPageVisible(authentication, "erfpacht"))
            assertTrue(service(properties, "bedrijf").isPortalPageVisible(authentication, "productdetail"))
            assertFalse(service(properties, "burger").isPortalPageVisible(authentication, "extern"))
            assertFalse(service(properties, "burger").isPortalPageVisible(authentication, "unknown"))
        }

    @Test
    fun `entry without audience is visible to every audience`() =
        runTest {
            val properties = properties(entry(name = "all", path = "/all"))

            listOf("burger", "bedrijf", FrontendMenuAudienceResolver.SUBJECT).forEach { audience ->
                assertEquals(listOf("all"), names(service(properties, audience)))
            }
        }

    @Test
    fun `burger entry is hidden for bedrijf and subject`() =
        runTest {
            val properties = properties(entry(name = "burger-only", path = "/burger", audience = listOf("burger")))

            assertEquals(listOf("burger-only"), names(service(properties, "burger")))
            assertEquals(emptyList<String>(), names(service(properties, "bedrijf")))
            assertEquals(emptyList<String>(), names(service(properties, FrontendMenuAudienceResolver.SUBJECT)))
        }

    @Test
    fun `subject entry is visible without a resolver`() =
        runTest {
            val properties = properties(entry(name = "subject-only", path = "/subject", audience = listOf("subject")))

            assertEquals(listOf("subject-only"), names(FrontendMenuService(properties, null, noConditions, listOf(portalPage, custom))))
        }

    @Test
    fun `audience match is case-insensitive`() =
        runTest {
            val properties = properties(entry(name = "bedrijf-only", path = "/bedrijf", audience = listOf("Bedrijf")))

            assertEquals(listOf("bedrijf-only"), names(service(properties, "BEDRIJF")))
        }

    @Test
    fun `config order is preserved and fields are copied`() =
        runTest {
            val properties =
                properties(
                    entry(name = "c", path = "/c", group = 2, ordering = 1),
                    entry(name = "a", type = "custom", path = "/a"),
                    entry(name = "b", path = "/b"),
                )

            val entries = FrontendMenuService(properties, null, noConditions, listOf(portalPage, custom)).getMenuEntries(authentication)

            assertEquals(listOf("c", "a", "b"), entries.map { it.name })
            assertEquals(2, entries[0].group)
            assertEquals(1, entries[0].ordering)
            assertEquals("custom", entries[1].route.type)
            assertEquals("/a", entries[1].route.path)
            assertEquals("page", entries[1].route.page)
            assertEquals("DocumentIcon", entries[1].icon)
        }

    @Test
    fun `every condition must hold`() =
        runTest {
            val checkers = FrontendMenuConditionalCheckers(listOf(checker("a", "yes"), checker("b", "yes")))
            val properties =
                properties(
                    entry(name = "both", path = "/both", conditional = mapOf("a" to "yes", "b" to "yes")),
                    entry(name = "first", path = "/first", conditional = mapOf("a" to "yes", "b" to "no")),
                    entry(name = "second", path = "/second", conditional = mapOf("a" to "no", "b" to "yes")),
                )

            assertEquals(listOf("both"), names(FrontendMenuService(properties, null, checkers, listOf(portalPage, custom))))
        }

    @Test
    fun `unknown condition key leaves the entry out`() =
        runTest {
            val properties = properties(entry(name = "unknown", path = "/unknown", conditional = mapOf("onbekend" to "x")))

            assertEquals(emptyList<String>(), names(FrontendMenuService(properties, null, noConditions, listOf(portalPage, custom))))
        }

    @Test
    fun `entry without conditions is unaffected by checkers`() =
        runTest {
            val checkers = FrontendMenuConditionalCheckers(listOf(checker("a", "yes")))
            val properties = properties(entry(name = "plain", path = "/plain"))

            assertEquals(listOf("plain"), names(FrontendMenuService(properties, null, checkers, listOf(portalPage, custom))))
        }

    @Test
    fun `first checker wins for a duplicate key`() =
        runTest {
            val checkers = FrontendMenuConditionalCheckers(listOf(checker("a", "yes"), checker("a", "no")))
            val properties = properties(entry(name = "dup", path = "/dup", conditional = mapOf("a" to "yes")))

            assertEquals(setOf("a"), checkers.duplicateKeys)
            assertEquals(listOf("dup"), names(FrontendMenuService(properties, null, checkers, listOf(portalPage, custom))))
        }

    @Test
    fun `portal page behind a failing condition is not visible`() =
        runTest {
            val checkers = FrontendMenuConditionalCheckers(listOf(checker("a", "yes")))
            val properties = properties(entry(name = "cond", path = "/cond", page = "cond", conditional = mapOf("a" to "no")))

            assertFalse(FrontendMenuService(properties, null, checkers, listOf(portalPage, custom)).isPortalPageVisible(authentication, "cond"))
        }

    private fun routeType(
        type: String,
        problems: (FrontendMenuEntryProperties) -> List<String> = { emptyList() },
    ) = object : FrontendMenuRouteType {
        override val type = type

        override fun problems(entry: FrontendMenuEntryProperties) = problems(entry)
    }

    private fun checker(
        key: String,
        holdsFor: String,
    ) = object : FrontendMenuConditionalChecker {
        override val key = key

        override suspend fun check(
            authentication: Authentication,
            value: String,
        ) = value == holdsFor
    }

    private fun service(
        properties: FrontendConfigurationProperties,
        audience: String,
    ) = FrontendMenuService(properties, { audience }, noConditions, listOf(portalPage, custom))

    private suspend fun names(service: FrontendMenuService) = service.getMenuEntries(authentication).map { it.name }

    private fun properties(vararg entries: FrontendMenuEntryProperties) = FrontendConfigurationProperties().apply { menuEntries = entries.toList() }

    private fun entry(
        name: String,
        path: String,
        type: String = "portal-page",
        page: String = "page",
        group: Int? = null,
        ordering: Int? = null,
        audience: List<String> = emptyList(),
        hidden: Boolean = false,
        conditional: Map<String, String> = emptyMap(),
    ) = FrontendMenuEntryProperties(
        name = name,
        group = group,
        ordering = ordering,
        audience = audience,
        hidden = hidden,
        conditional = conditional,
        route = FrontendMenuRouteProperties(type = type, page = page, path = path),
    )
}