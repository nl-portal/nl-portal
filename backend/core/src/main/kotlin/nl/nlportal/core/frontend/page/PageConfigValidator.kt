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
import nl.nlportal.core.frontend.page.resolver.PageDataResolver
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateSource

/**
 * Checks `nl-portal.frontend.pages` configuration. A page with problems answers 404;
 * a binding with problems answers `error: true` while the rest of the page still renders.
 */
class PageConfigValidator(
    private val resolvers: List<PageDataResolver>,
    private val projection: PageProjection,
    private val templateSources: List<PageTemplateSource> = emptyList(),
) {
    /** Problems of page [key] itself, including its template and a canonical clash with another key in [allKeys]. */
    fun pageProblems(
        key: String,
        page: FrontendPageProperties,
        allKeys: Collection<String>,
    ): List<String> =
        buildList {
            if (allKeys.count { PageParams.canonical(it) == PageParams.canonical(key) } > 1) {
                add("page key must be unique ignoring case and dashes")
            }
            when (page.type) {
                STATIC -> if (page.bindings.isNotEmpty()) add("static page must not have bindings")
                DYNAMIC -> if (page.bindings.isEmpty()) add("dynamic page requires at least one binding")
                else -> add("type must be one of ${PAGE_TYPES.joinToString()}, was '${page.type}'")
            }
            addAll(templateProblems(page))
        }

    private fun templateProblems(page: FrontendPageProperties): List<String> =
        buildList {
            val template = page.template
            if (template == null) {
                add("page requires a template")
                return@buildList
            }
            if (template.source == BINDING_SOURCE) {
                when {
                    page.type == STATIC -> add("static page can't use the $BINDING_SOURCE template source")
                    template.binding.isNullOrBlank() -> add("template.binding is required for the $BINDING_SOURCE template source")
                    template.binding !in page.bindings.keys -> add("template.binding '${template.binding}' is not a binding of this page")
                }
                return@buildList
            }
            if (template.binding != null) add("template.binding is only allowed with the $BINDING_SOURCE template source")
            val matching = templateSources.filter { it.type == template.source }
            when (matching.size) {
                0 -> add("unknown template source '${template.source}'")
                1 -> addAll(sourceProblems(matching.single(), PageParams(template.params)).map { "template: $it" })
                else -> add("template source '${template.source}' is provided by ${matching.size} beans")
            }
        }

    private fun sourceProblems(
        source: PageTemplateSource,
        params: PageParams,
    ): List<String> =
        try {
            source.validate(params)
        } catch (e: IllegalArgumentException) {
            listOf(e.message ?: "invalid params")
        }

    /** Problems of binding [alias]. */
    fun bindingProblems(
        alias: String,
        binding: FrontendPageBindingProperties,
    ): List<String> =
        buildList {
            if (!ALIAS_PATTERN.matches(alias) || alias in RESERVED_ALIASES) {
                add("alias must match ${ALIAS_PATTERN.pattern} and not be one of ${RESERVED_ALIASES.joinToString()}")
            }
            val matching = resolvers.filter { it.type == binding.resolver }
            when (matching.size) {
                0 -> {
                    add("unknown resolver '${binding.resolver}'")
                }

                1 -> {
                    val fixed = matching.single().fixedParams.map { PageParams.canonical(it) }
                    binding.overrides
                        .filter { PageParams.canonical(it) in fixed }
                        .forEach { add("override '$it' is a fixed param of resolver '${binding.resolver}'") }
                }

                else -> {
                    add("resolver '${binding.resolver}' is provided by ${matching.size} beans")
                }
            }
            binding.params.keys
                .groupBy { PageParams.canonical(it) }
                .filterValues { it.size > 1 }
                .values
                .forEach { add("params ${it.joinToString()} are the same key ignoring case and dashes") }
            compileProblem("projection", binding.projection)?.let { add(it) }
        }

    /** Every problem of every page and binding, for logging at startup. */
    fun allProblems(pages: Map<String, FrontendPageProperties>): List<String> =
        pages.entries.sortedBy { it.key }.flatMap { (key, page) ->
            pageProblems(key, page, pages.keys).map { "pages.$key: $it" } +
                page.bindings.entries.sortedBy { it.key }.flatMap { (alias, binding) ->
                    bindingProblems(alias, binding).map { "pages.$key.bindings.$alias: $it" }
                }
        }

    private fun compileProblem(
        name: String,
        expression: String,
    ): String? =
        try {
            projection.compile(expression)
            null
        } catch (e: Exception) {
            "$name does not compile: ${e.message}"
        }

    companion object {
        const val STATIC = "static"
        const val DYNAMIC = "dynamic"
        const val BINDING_SOURCE = "binding"
        val PAGE_TYPES = listOf(STATIC, DYNAMIC)
        private val ALIAS_PATTERN = Regex("^[a-z][a-z0-9]*$")
        private val RESERVED_ALIASES = setOf("errors", "meta")
    }
}