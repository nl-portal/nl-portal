/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.frontend.page

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuRouteProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageTemplateProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.page.FakePageDataResolver.Companion.json
import nl.nlportal.core.frontend.page.resolver.PageData
import nl.nlportal.core.frontend.page.resolver.PageDataContext
import nl.nlportal.core.frontend.page.resolver.PageDataResolver
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.core.frontend.page.template.PageTemplateLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.TestingAuthenticationToken
import java.util.Locale

internal class PageServiceTest {
    private val context = PageDataContext(TestingAuthenticationToken("user", null), Locale.forLanguageTag("nl"))
    private val fake =
        FakePageDataResolver(
            objects =
                mapOf(
                    "owned" to PageData(json("""{"naam":"Erfpacht","eigenaren":["123"]}"""), template = "{{ naam }}"),
                ),
        )
    private val other = FakePageDataResolver(type = "other", objects = mapOf("owned" to PageData(json("""{"x":1}""")), "only-other" to PageData(json("{}"))))
    private val noById =
        object : PageDataResolver {
            override val type = "nobyid"

            override suspend fun resolve(
                context: PageDataContext,
                params: PageParams,
            ) = PageData(json("{}"))
        }

    private val templates =
        FakePageTemplateSource(
            documents =
                mapOf(
                    "over-ons" to
                        PageTemplateDocument(
                            "Over ons",
                            mapOf("nl-NL" to mapOf("titel" to "Over ons", "intro" to "Welkom"), "en-GB" to mapOf("titel" to "About us")),
                        ),
                    "erfpacht" to PageTemplateDocument("{{ producten }}"),
                    "x" to PageTemplateDocument("x"),
                ),
        )

    private val templateLoader = PageTemplateLoader(listOf(templates))

    private val listBinding =
        FrontendPageBindingProperties(
            resolver = "fake",
            params = linkedMapOf("page" to "0", "source" to "fixed"),
            overrides = listOf("page", "filter.status"),
            projection = "{page, status}",
        )

    private val pages =
        mapOf(
            "over-ons" to FrontendPageProperties(type = "static", template = template("over-ons")),
            "erfpacht" to
                FrontendPageProperties(
                    type = "dynamic",
                    template = template("erfpacht"),
                    bindings =
                        linkedMapOf(
                            "zz" to FrontendPageBindingProperties(resolver = "fake", projection = ".count"),
                            "producten" to listBinding,
                            "broken" to FrontendPageBindingProperties(resolver = "fake", projection = "{ naam"),
                            "aa" to FrontendPageBindingProperties(resolver = "nope"),
                        ),
                ),
            "product" to
                FrontendPageProperties(
                    type = "dynamic",
                    template = FrontendPageTemplateProperties(source = "binding", binding = "product"),
                    bindings =
                        linkedMapOf(
                            "product" to FrontendPageBindingProperties(resolver = "fake", projection = "{naam}"),
                        ),
                ),
            "multi" to
                FrontendPageProperties(
                    type = "dynamic",
                    template = FrontendPageTemplateProperties(source = "binding", binding = "a"),
                    bindings =
                        linkedMapOf(
                            "b" to FrontendPageBindingProperties(resolver = "other", projection = "."),
                            "a" to FrontendPageBindingProperties(resolver = "fake", projection = "{naam}"),
                        ),
                ),
            "unsupported" to
                FrontendPageProperties(
                    type = "dynamic",
                    template = FrontendPageTemplateProperties(source = "binding", binding = "x"),
                    bindings = linkedMapOf("x" to FrontendPageBindingProperties(resolver = "nobyid")),
                ),
            "invalid" to FrontendPageProperties(type = "static"),
            "verborgen" to FrontendPageProperties(type = "static", template = template("x")),
            "bedrijf" to FrontendPageProperties(type = "static", template = template("x")),
            "missing" to FrontendPageProperties(type = "static", template = template("missing")),
        )

    private val menuEntries =
        listOf(
            entry("over-ons", "Over-Ons", "/over-ons"),
            entry("erfpacht", "erfpacht", "/erfpacht"),
            entry("product", "product", "/erfpacht/:id", hidden = true),
            entry("multi", "multi", "/multi/:id", hidden = true),
            entry("unsupported", "unsupported", "/unsupported/:id", hidden = true),
            entry("invalid", "invalid", "/invalid"),
            entry("missing", "missing", "/missing"),
            entry("bedrijf", "bedrijf", "/bedrijf", audience = listOf("bedrijf")),
            entry("custom", "verborgen", "/custom/*", type = "custom"),
        )

    private fun service(audience: String = "burger"): PageService {
        val properties =
            FrontendConfigurationProperties().apply {
                this.menuEntries = this@PageServiceTest.menuEntries
                this.pages = this@PageServiceTest.pages
            }
        val resolvers = listOf(fake, other, noById)
        val projection = PageProjection()
        return PageService(
            properties,
            FrontendMenuService(properties) { audience },
            resolvers,
            projection,
            PageConfigValidator(resolvers, projection, listOf(templates)),
            templateLoader,
        )
    }

    @Test
    fun `static page returns its template and all messages`() =
        runTest {
            val page = service().getPage(context, "over-ons")

            assertEquals("static", page.type)
            assertEquals("Over ons", page.template)
            assertEquals(mapOf("nl-NL" to mapOf("titel" to "Over ons", "intro" to "Welkom"), "en-GB" to mapOf("titel" to "About us")), page.messages)
            assertNull(page.root)
            assertEquals(emptyList<Any>(), page.bindings)
        }

    @Test
    fun `templates are loaded on every request`() =
        runTest {
            service().getPage(context, "over-ons")
            service().getPage(context, "over-ons")

            assertEquals(2, templates.loads)
        }

    @Test
    fun `page key is matched canonically`() =
        runTest {
            assertEquals("Over ons", service().getPage(context, "OVERONS").template)
        }

    @Test
    fun `page is only served through a templated menu entry visible to the audience`() =
        runTest {
            assertThrows<PageNotFoundException> { service("burger").getPage(context, "bedrijf") }
            assertEquals("x", service("bedrijf").getPage(context, "bedrijf").template)
            assertThrows<PageNotFoundException> { service().getPage(context, "verborgen") }
        }

    @Test
    fun `page reachable only through a hidden entry is served`() =
        runTest {
            assertEquals("dynamic", service().getPage(context, "product", "owned").type)
        }

    @Test
    fun `every page-level failure is a 404`() =
        runTest {
            assertThrows<PageNotFoundException> { service().getPage(context, "unknown") }
            assertThrows<PageNotFoundException> { service().getPage(context, "invalid") }
            assertThrows<PageNotFoundException> { service().getPage(context, "missing") }
            assertThrows<PageNotFoundException> { service().getPage(context, "over-ons", "owned") }
        }

    @Test
    fun `a template that fails to load is a 404`() =
        runTest {
            templates.failure = IllegalStateException("down")

            assertThrows<PageNotFoundException> { service().getPage(context, "over-ons") }
        }

    @Test
    fun `bindings are sorted by alias and errors are contained per binding`() =
        runTest {
            val page = service().getPage(context, "erfpacht")

            assertEquals(listOf("aa", "broken", "producten", "zz"), page.bindings.map { it.alias })
            assertEquals(listOf(true, true, false, false), page.bindings.map { it.error })
            assertNull(page.bindings[0].data)
            assertEquals(json("""{"page":"0","status":null}"""), page.bindings[2].data)
            assertEquals(json("23"), page.bindings[3].data)
        }

    @Test
    fun `a failing resolver only fails its bindings`() =
        runTest {
            fake.failure = IllegalStateException("down")

            val page = service().getPage(context, "erfpacht")

            assertTrue(page.bindings.all { it.error })
            assertEquals("Over ons", service().getPage(context, "over-ons").template)
        }

    @Test
    fun `meta params hold the effective override values`() =
        runTest {
            val producten = service().getPage(context, "erfpacht").bindings.single { it.alias == "producten" }

            assertEquals(mapOf("page" to "0"), producten.meta.params)
        }

    @Test
    fun `refetch applies allowed overrides with canonical keys`() =
        runTest {
            val response = service().refetch(context, "erfpacht", "producten", mapOf("PAGE" to "2", "filter.status" to "actief"))

            assertFalse(response.error)
            assertEquals(json("""{"page":"2","status":"actief"}"""), response.data)
            assertEquals(mapOf("page" to "2", "filter.status" to "actief"), response.meta.params)
            assertEquals("fixed", fake.received.last().string("source"))
        }

    @Test
    fun `refetch with a blank value resets to the default`() =
        runTest {
            val response = service().refetch(context, "erfpacht", "producten", mapOf("page" to "", "filter.status" to ""))

            assertEquals(json("""{"page":"0","status":null}"""), response.data)
            assertEquals(mapOf("page" to "0"), response.meta.params)
        }

    @Test
    fun `refetch rejects keys outside the allowlist before resolving`() =
        runTest {
            assertThrows<PageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("source" to "other")) }
            assertThrows<PageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("size" to "1000")) }
            assertEquals(emptyList<PageParams>(), fake.received)
        }

    @Test
    fun `refetch rejects params the resolver doesn't validate`() =
        runTest {
            assertThrows<PageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("page" to "-1")) }
            assertThrows<PageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("page" to "een")) }
            assertEquals(emptyList<PageParams>(), fake.received)
        }

    @Test
    fun `refetch of an unknown, invalid or non-dynamic binding is a 404`() =
        runTest {
            assertThrows<PageNotFoundException> { service().refetch(context, "erfpacht", "onbekend", emptyMap()) }
            assertThrows<PageNotFoundException> { service().refetch(context, "erfpacht", "broken", emptyMap()) }
            assertThrows<PageNotFoundException> { service().refetch(context, "over-ons", "x", emptyMap()) }
            assertThrows<PageNotFoundException> { service("burger").refetch(context, "bedrijf", "x", emptyMap()) }
        }

    @Test
    fun `refetch of a failing resolver returns an error binding`() =
        runTest {
            fake.failure = IllegalStateException("down")

            val response = service().refetch(context, "erfpacht", "producten", mapOf("page" to "1"))

            assertTrue(response.error)
            assertEquals(mapOf("page" to "1"), response.meta.params)
        }

    @Test
    fun `page with an id takes its template from the binding and renders against its data`() =
        runTest {
            val page = service().getPage(context, "product", "owned")

            assertEquals("{{ naam }}", page.template)
            assertEquals("product", page.root)
            assertEquals(emptyMap<String, Map<String, String>>(), page.messages)
            assertEquals(json("""{"naam":"Erfpacht"}"""), page.bindings.single().data)
        }

    @Test
    fun `binding template without an id comes from the list resolve`() =
        runTest {
            val page = service().getPage(context, "product")

            assertEquals("product", page.root)
            assertNull(page.template)
            assertFalse(page.bindings.single().error)
        }

    @Test
    fun `page with an id is a 404 when any binding doesn't find the object`() =
        runTest {
            assertThrows<PageNotFoundException> { service().getPage(context, "product", "someone-else") }
            assertThrows<PageNotFoundException> { service().getPage(context, "multi", "only-other") }
        }

    @Test
    fun `bindings of a page with an id are sorted by alias`() =
        runTest {
            assertEquals(listOf("a", "b"), service().getPage(context, "multi", "owned").bindings.map { it.alias })
        }

    @Test
    fun `binding of a resolver without id support is an error and leaves the template empty`() =
        runTest {
            val page = service().getPage(context, "unsupported", "owned")

            assertTrue(page.bindings.single().error)
            assertNull(page.template)
            assertEquals("x", page.root)
        }

    private fun template(name: String) = FrontendPageTemplateProperties(source = "fake", params = mapOf("name" to name))

    private fun entry(
        name: String,
        page: String,
        path: String,
        type: String = "templated",
        hidden: Boolean = false,
        audience: List<String> = emptyList(),
    ) = FrontendMenuEntryProperties(
        name = name,
        audience = audience,
        hidden = hidden,
        route = FrontendMenuRouteProperties(type = type, page = page, path = path),
    )
}