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
package nl.nlportal.core.frontend.portalpage

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuRouteProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageTemplateProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.menu.service.FrontendMenuConditionalCheckers
import nl.nlportal.core.frontend.portalpage.FakePortalPageBindingResolver.Companion.json
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingData
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import java.util.Locale

@ExtendWith(OutputCaptureExtension::class)
internal class PortalPageServiceTest {
    private val context = PortalPageRequestContext(TestingAuthenticationToken("user", null), Locale.forLanguageTag("nl"))
    private val fake =
        FakePortalPageBindingResolver(
            objects =
                mapOf(
                    "owned" to PortalPageBindingData(json("""{"naam":"Erfpacht","eigenaren":["123"]}"""), template = "{{ naam }}"),
                ),
        )
    private val other = FakePortalPageBindingResolver(type = "other", objects = mapOf("owned" to PortalPageBindingData(json("""{"x":1}""")), "only-other" to PortalPageBindingData(json("{}"))))
    private val noById =
        object : PortalPageBindingResolver {
            override val type = "nobyid"

            override suspend fun resolve(
                context: PortalPageRequestContext,
                params: PortalPageParams,
            ) = PortalPageBindingData(json("{}"))
        }

    private val templates =
        FakePortalPageTemplateSource(
            templates =
                mapOf(
                    "over-ons" to
                        PortalPageTemplate(
                            "Over ons",
                            mapOf("nl-NL" to mapOf("titel" to "Over ons", "intro" to "Welkom"), "en-GB" to mapOf("titel" to "About us")),
                        ),
                    "erfpacht" to PortalPageTemplate("{{ producten }}"),
                    "x" to PortalPageTemplate("x"),
                ),
        )

    private val templateLoader = PortalPageTemplateLoader(listOf(templates))

    private val listBinding =
        PortalPageBindingProperties(
            resolver = "fake",
            params = linkedMapOf("page" to "0", "source" to "fixed"),
            overrides = listOf("page", "filter.status"),
            projection = "{page, status}",
        )

    private val pages =
        mapOf(
            "over-ons" to PortalPageProperties(type = "static", template = template("over-ons")),
            "erfpacht" to
                PortalPageProperties(
                    type = "dynamic",
                    template = template("erfpacht"),
                    bindings =
                        linkedMapOf(
                            "zz" to PortalPageBindingProperties(resolver = "fake", projection = ".count"),
                            "producten" to listBinding,
                            "broken" to PortalPageBindingProperties(resolver = "fake", projection = "{ naam"),
                            "aa" to PortalPageBindingProperties(resolver = "nope"),
                        ),
                ),
            "product" to
                PortalPageProperties(
                    type = "dynamic",
                    template = PortalPageTemplateProperties(source = "binding", binding = "product"),
                    bindings =
                        linkedMapOf(
                            "product" to PortalPageBindingProperties(resolver = "fake", projection = "{naam}"),
                        ),
                ),
            "multi" to
                PortalPageProperties(
                    type = "dynamic",
                    template = PortalPageTemplateProperties(source = "binding", binding = "a"),
                    bindings =
                        linkedMapOf(
                            "b" to PortalPageBindingProperties(resolver = "other", projection = "."),
                            "a" to PortalPageBindingProperties(resolver = "fake", projection = "{naam}"),
                        ),
                ),
            "unsupported" to
                PortalPageProperties(
                    type = "dynamic",
                    template = PortalPageTemplateProperties(source = "binding", binding = "x"),
                    bindings = linkedMapOf("x" to PortalPageBindingProperties(resolver = "nobyid")),
                ),
            "invalid" to PortalPageProperties(type = "static"),
            "verborgen" to PortalPageProperties(type = "static", template = template("x")),
            "bedrijf" to PortalPageProperties(type = "static", template = template("x")),
            "conditioneel" to PortalPageProperties(type = "static", template = template("x")),
            "missing" to PortalPageProperties(type = "static", template = template("missing")),
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
            entry("conditioneel", "conditioneel", "/conditioneel", conditional = mapOf("onbekend" to "x")),
            entry("custom", "verborgen", "/custom/*", type = "custom"),
        )

    private fun service(audience: String = "burger"): PortalPageService {
        val properties =
            FrontendConfigurationProperties().apply {
                this.menuEntries = this@PortalPageServiceTest.menuEntries
                this.pages = this@PortalPageServiceTest.pages
            }
        val resolvers = listOf(fake, other, noById)
        val projection = PortalPageProjection()
        val validator = PortalPageConfigValidator(resolvers, projection, listOf(templates))
        return PortalPageService(
            properties,
            FrontendMenuService(properties, { audience }, FrontendMenuConditionalCheckers(emptyList()), listOf(PortalPageMenuRouteType(properties, validator))),
            resolvers,
            projection,
            validator,
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
    fun `page is only served through a portal page menu entry visible to the audience`() =
        runTest {
            assertThrows<PortalPageNotFoundException> { service("burger").getPage(context, "bedrijf") }
            assertEquals("x", service("bedrijf").getPage(context, "bedrijf").template)
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "verborgen") }
        }

    @Test
    fun `page behind a failing menu condition is not found`() =
        runTest {
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "conditioneel") }
        }

    @Test
    fun `page reachable only through a hidden entry is served`() =
        runTest {
            assertEquals("dynamic", service().getPage(context, "product", "owned").type)
        }

    @Test
    fun `every page-level failure is a 404`() =
        runTest {
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "unknown") }
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "invalid") }
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "missing") }
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "over-ons", "owned") }
        }

    @Test
    fun `a template that fails to load is a 404`() =
        runTest {
            templates.failure = IllegalStateException("down")

            assertThrows<PortalPageNotFoundException> { service().getPage(context, "over-ons") }
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
    fun `configuration problems aren't logged at request time, failing resolvers are`(output: CapturedOutput) =
        runTest {
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "invalid") }
            service().getPage(context, "erfpacht")
            assertFalse(output.all.contains("Binding 'broken'"), output.all)
            assertFalse(output.all.contains("Binding 'aa'"), output.all)

            fake.failure = IllegalStateException("down for BSN 999993847")
            service().getPage(context, "erfpacht")
            assertTrue(
                output.all.lines().any { it.contains("WARN") && it.contains("Binding 'producten' of page 'erfpacht' (resolver 'fake') failed: IllegalStateException") },
                output.all,
            )
            assertFalse(output.all.contains("999993847"), output.all)
        }

    @Test
    fun `a missing template is a 404 with a warning`(output: CapturedOutput) =
        runTest {
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "missing") }

            assertTrue(
                output.all.lines().any { it.contains("WARN") && it.contains("Template of portal page 'missing' (source 'fake') not found; the page answers 404") },
                output.all,
            )
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
            assertThrows<PortalPageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("source" to "other")) }
            assertThrows<PortalPageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("size" to "1000")) }
            assertEquals(emptyList<PortalPageParams>(), fake.received)
        }

    @Test
    fun `refetch rejects params the resolver doesn't validate`() =
        runTest {
            assertThrows<PortalPageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("page" to "-1")) }
            assertThrows<PortalPageOverrideException> { service().refetch(context, "erfpacht", "producten", mapOf("page" to "een")) }
            assertEquals(emptyList<PortalPageParams>(), fake.received)
        }

    @Test
    fun `refetch of an unknown, invalid or non-dynamic binding is a 404`() =
        runTest {
            assertThrows<PortalPageNotFoundException> { service().refetch(context, "erfpacht", "onbekend", emptyMap()) }
            assertThrows<PortalPageNotFoundException> { service().refetch(context, "erfpacht", "broken", emptyMap()) }
            assertThrows<PortalPageNotFoundException> { service().refetch(context, "over-ons", "x", emptyMap()) }
            assertThrows<PortalPageNotFoundException> { service("burger").refetch(context, "bedrijf", "x", emptyMap()) }
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
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "product", "someone-else") }
            assertThrows<PortalPageNotFoundException> { service().getPage(context, "multi", "only-other") }
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

    private fun template(name: String) = PortalPageTemplateProperties(source = "fake", params = mapOf("name" to name))

    private fun entry(
        name: String,
        page: String,
        path: String,
        type: String = "portal-page",
        hidden: Boolean = false,
        audience: List<String> = emptyList(),
        conditional: Map<String, String> = emptyMap(),
    ) = FrontendMenuEntryProperties(
        name = name,
        audience = audience,
        hidden = hidden,
        conditional = conditional,
        route = FrontendMenuRouteProperties(type = type, page = page, path = path),
    )
}