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

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageTemplateProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PageConfigValidatorTest {
    private val validator =
        PageConfigValidator(
            listOf(FakePageDataResolver(), FakePageDataResolver(type = "twice"), FakePageDataResolver(type = "twice")),
            PageProjection(),
            listOf(FakePageTemplateSource(), FakePageTemplateSource(type = "double"), FakePageTemplateSource(type = "double")),
        )

    @Test
    fun `valid pages have no problems`() {
        assertNoProblems(validator.pageProblems("over-ons", FrontendPageProperties(type = "static", template = template()), listOf("over-ons")))
        assertNoProblems(
            validator.pageProblems("erfpacht", FrontendPageProperties(type = "dynamic", template = template(), bindings = mapOf("a" to binding())), listOf("erfpacht")),
        )
        assertNoProblems(
            validator.pageProblems(
                "product",
                FrontendPageProperties(type = "dynamic", template = FrontendPageTemplateProperties(source = "binding", binding = "a"), bindings = mapOf("a" to binding())),
                listOf("product"),
            ),
        )
    }

    @Test
    fun `type must be known`() {
        assertProblem("type must be one of static, dynamic, was 'sourced'", page(FrontendPageProperties(type = "sourced", template = template())))
    }

    @Test
    fun `static has no bindings and dynamic at least one`() {
        assertProblem("static page must not have bindings", page(FrontendPageProperties(type = "static", template = template(), bindings = mapOf("a" to binding()))))
        assertProblem("dynamic page requires at least one binding", page(FrontendPageProperties(type = "dynamic", template = template())))
    }

    @Test
    fun `every page requires a template from a known source`() {
        assertProblem("page requires a template", page(FrontendPageProperties(type = "static")))
        assertProblem("unknown template source 'nope'", page(FrontendPageProperties(type = "static", template = template(source = "nope"))))
        assertProblem("template source 'double' is provided by 2 beans", page(FrontendPageProperties(type = "static", template = template(source = "double"))))
    }

    @Test
    fun `template params are validated by the source`() {
        assertProblem("template: param 'name' is required", page(FrontendPageProperties(type = "static", template = template(params = emptyMap()))))
    }

    @Test
    fun `binding source needs a binding of a dynamic page`() {
        val bindingSource = FrontendPageTemplateProperties(source = "binding", binding = "a")
        assertProblem("static page can't use the binding template source", page(FrontendPageProperties(type = "static", template = bindingSource)))
        assertProblem(
            "template.binding is required for the binding template source",
            page(FrontendPageProperties(type = "dynamic", template = FrontendPageTemplateProperties(source = "binding"), bindings = mapOf("a" to binding()))),
        )
        assertProblem(
            "template.binding 'a' is not a binding of this page",
            page(FrontendPageProperties(type = "dynamic", template = bindingSource, bindings = mapOf("b" to binding()))),
        )
    }

    @Test
    fun `template binding is only allowed with the binding source`() {
        assertProblem(
            "template.binding is only allowed with the binding template source",
            page(FrontendPageProperties(type = "static", template = template().apply { binding = "a" })),
        )
    }

    @Test
    fun `page keys must be unique in canonical form`() {
        assertProblem(
            "page key must be unique ignoring case and dashes",
            validator.pageProblems("over-ons", FrontendPageProperties(type = "static", template = template()), listOf("over-ons", "overons")),
        )
    }

    @Test
    fun `alias must be a lowercase variable name and not reserved`() {
        listOf("A", "1a", "a-b", "a_b", "errors", "meta").forEach { alias ->
            assertTrue(validator.bindingProblems(alias, binding()).any { it.startsWith("alias must match") }, alias)
        }
        assertNoProblems(validator.bindingProblems("producten2", binding()))
    }

    @Test
    fun `resolver must be provided by exactly one bean`() {
        assertProblem("unknown resolver 'nope'", validator.bindingProblems("a", binding(resolver = "nope")))
        assertProblem("resolver 'twice' is provided by 2 beans", validator.bindingProblems("a", binding(resolver = "twice")))
    }

    @Test
    fun `overrides must not contain fixed params, compared canonically`() {
        assertProblem("override 'SOURCE' is a fixed param of resolver 'fake'", validator.bindingProblems("a", binding(overrides = listOf("page", "SOURCE"))))
    }

    @Test
    fun `an override key needs no default`() {
        assertNoProblems(validator.bindingProblems("a", binding(params = mapOf("size" to "10"), overrides = listOf("filter.status"))))
    }

    @Test
    fun `params keys must be unique in canonical form`() {
        assertProblem(
            "params product-type-code, PRODUCTTYPECODE are the same key ignoring case and dashes",
            validator.bindingProblems("a", binding(params = mapOf("product-type-code" to "a", "PRODUCTTYPECODE" to "b"))),
        )
    }

    @Test
    fun `projection must compile`() {
        assertTrue(validator.bindingProblems("a", binding(projection = "{ naam")).single().startsWith("projection does not compile"))
    }

    @Test
    fun `allProblems lists every page and binding problem with its path`() {
        val problems =
            validator.allProblems(
                mapOf(
                    "b" to FrontendPageProperties(type = "dynamic", template = template(), bindings = mapOf("x" to binding(resolver = "nope"))),
                    "a" to FrontendPageProperties(type = "static"),
                ),
            )

        assertEquals(
            listOf(
                "pages.a: page requires a template",
                "pages.b.bindings.x: unknown resolver 'nope'",
            ),
            problems,
        )
    }

    private fun page(page: FrontendPageProperties) = validator.pageProblems("p", page, listOf("p"))

    private fun template(
        source: String = "fake",
        params: Map<String, String> = mapOf("name" to "x"),
    ) = FrontendPageTemplateProperties(source = source, params = params)

    private fun binding(
        resolver: String = "fake",
        params: Map<String, String> = emptyMap(),
        overrides: List<String> = emptyList(),
        projection: String = "{}",
    ) = FrontendPageBindingProperties(resolver = resolver, params = params, overrides = overrides, projection = projection)

    private fun assertNoProblems(problems: List<String>) = assertEquals(emptyList<String>(), problems)

    private fun assertProblem(
        expected: String,
        problems: List<String>,
    ) = assertTrue(expected in problems, "Expected '$expected' in $problems")
}