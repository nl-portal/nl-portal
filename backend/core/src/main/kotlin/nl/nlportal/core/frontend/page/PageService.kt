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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendPageProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.page.PageConfigValidator.Companion.DYNAMIC
import nl.nlportal.core.frontend.page.PageConfigValidator.Companion.BINDING_SOURCE
import nl.nlportal.core.frontend.page.PageConfigValidator.Companion.STATIC
import nl.nlportal.core.frontend.page.domain.BindingMeta
import nl.nlportal.core.frontend.page.domain.BindingResponse
import nl.nlportal.core.frontend.page.domain.PageResponse
import nl.nlportal.core.frontend.page.resolver.PageData
import nl.nlportal.core.frontend.page.resolver.PageDataContext
import nl.nlportal.core.frontend.page.resolver.PageDataResolver
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.core.frontend.page.template.PageTemplateLoader
import kotlin.coroutines.cancellation.CancellationException

/**
 * Serves the configured `nl-portal.frontend.pages` to users who can reach them through a `templated` menu entry.
 * Every request validates the page and its bindings; a problem with one binding never affects the others.
 */
class PageService(
    private val properties: FrontendConfigurationProperties,
    private val menuService: FrontendMenuService,
    private val resolvers: List<PageDataResolver>,
    private val projection: PageProjection,
    private val validator: PageConfigValidator,
    private val templateLoader: PageTemplateLoader,
) {
    /**
     * A page with its template and all bindings resolved. With an [id], every binding resolves that object instead of a list.
     *
     * @throws PageNotFoundException when the page isn't visible or isn't valid, its template is missing or fails to load,
     * a static page is requested with an id, or any binding doesn't find [id] for the user
     */
    suspend fun getPage(
        context: PageDataContext,
        key: String,
        id: String? = null,
    ): PageResponse {
        val (pageKey, page) = findPage(context, key)
        if (id != null && page.type == STATIC) throw PageNotFoundException("Static page '$pageKey' has no id")
        val template = page.template!!
        val document = if (template.source == BINDING_SOURCE) null else loadDocument(pageKey, template.source, template.params)

        val bindings =
            resolveBindings(pageKey, page) { alias, binding, resolver, params ->
                if (id == null) {
                    toResolved(alias, binding, params, resolver.resolve(context, params))
                } else {
                    resolver.resolve(context, params, id)?.let { toResolved(alias, binding, params, it) }
                }
            }
        if (bindings.any { it == null }) throw PageNotFoundException("Object not found for page '$pageKey'")
        val resolved = bindings.map { it!! }

        val root = template.binding.takeIf { template.source == BINDING_SOURCE }
        return PageResponse(
            type = page.type,
            template = if (root == null) document!!.template else resolved.single { it.response.alias == root }.template,
            messages = document?.messages.orEmpty(),
            root = root,
            bindings = resolved.map { it.response },
        )
    }

    private suspend fun loadDocument(
        pageKey: String,
        source: String,
        params: Map<String, String>,
    ): PageTemplateDocument {
        val document =
            try {
                templateLoader.load(source, params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Template of page '$pageKey' (source '$source') failed to load" }
                null
            }
        return document ?: throw PageNotFoundException("Template of page '$pageKey' not found")
    }

    /**
     * Resolves binding [alias] of a dynamic page again with [overrides] applied to its params.
     * A blank override value resets the key to its configured value.
     *
     * @throws PageNotFoundException when the page or binding isn't visible, isn't valid or the page isn't dynamic
     * @throws PageOverrideException when an override key isn't allowed or the merged params are invalid
     */
    suspend fun refetch(
        context: PageDataContext,
        key: String,
        alias: String,
        overrides: Map<String, String>,
    ): BindingResponse {
        val (pageKey, page) = findPage(context, key)
        if (page.type != DYNAMIC) throw PageNotFoundException("Page '$pageKey' is not a dynamic page")
        val binding = page.bindings[alias] ?: throw PageNotFoundException("Page '$pageKey' has no binding '$alias'")
        if (validator.bindingProblems(alias, binding).isNotEmpty()) throw PageNotFoundException("Binding '$alias' of page '$pageKey' is invalid")

        val allowed = binding.overrides.map { PageParams.canonical(it) }.toSet()
        val notAllowed = overrides.keys.filter { PageParams.canonical(it) !in allowed }
        if (notAllowed.isNotEmpty()) throw PageOverrideException("Params not allowed: ${notAllowed.joinToString()}")

        val params = PageParams(binding.params).withOverrides(overrides)
        val resolver = resolverFor(binding)
        val problems = validate(resolver, params)
        if (problems.isNotEmpty()) throw PageOverrideException("Invalid params: ${problems.joinToString()}")

        return try {
            toResolved(alias, binding, params, resolver.resolve(context, params)).response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Binding '$alias' of page '$pageKey' (resolver '${binding.resolver}') failed" }
            errorResponse(alias, binding, params)
        }
    }

    private fun findPage(
        context: PageDataContext,
        key: String,
    ): Pair<String, FrontendPageProperties> {
        val matches = properties.pages.entries.filter { PageParams.canonical(it.key) == PageParams.canonical(key) }
        val (pageKey, page) = matches.singleOrNull() ?: throw PageNotFoundException("Page '$key' not found")
        if (!menuService.isTemplatedPageVisible(context.authentication, pageKey)) throw PageNotFoundException("Page '$key' not visible")
        if (validator.pageProblems(pageKey, page, properties.pages.keys).isNotEmpty()) throw PageNotFoundException("Page '$key' is invalid")
        return pageKey to page
    }

    private suspend fun resolveBindings(
        pageKey: String,
        page: FrontendPageProperties,
        resolve: suspend (String, FrontendPageBindingProperties, PageDataResolver, PageParams) -> ResolvedBinding?,
    ): List<ResolvedBinding?> =
        coroutineScope {
            page.bindings.entries
                .sortedBy { it.key }
                .map { (alias, binding) ->
                    async {
                        val params = PageParams(binding.params)
                        try {
                            val problems = validator.bindingProblems(alias, binding)
                            check(problems.isEmpty()) { problems.joinToString() }
                            val resolver = resolverFor(binding)
                            val paramProblems = validate(resolver, params)
                            check(paramProblems.isEmpty()) { paramProblems.joinToString() }
                            resolve(alias, binding, resolver, params)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.warn(e) { "Binding '$alias' of page '$pageKey' (resolver '${binding.resolver}') failed" }
                            ResolvedBinding(errorResponse(alias, binding, params), null)
                        }
                    }
                }.awaitAll()
        }

    private fun resolverFor(binding: FrontendPageBindingProperties): PageDataResolver = resolvers.single { it.type == binding.resolver }

    private fun validate(
        resolver: PageDataResolver,
        params: PageParams,
    ): List<String> =
        try {
            resolver.validate(params)
        } catch (e: IllegalArgumentException) {
            listOf(e.message ?: "invalid params")
        }

    private fun toResolved(
        alias: String,
        binding: FrontendPageBindingProperties,
        params: PageParams,
        pageData: PageData,
    ) = ResolvedBinding(
        response =
            BindingResponse(
                alias = alias,
                data = projection.apply(binding.projection, pageData.data),
                error = false,
                meta = BindingMeta(params = metaParams(binding, params)),
            ),
        template = pageData.template,
    )

    private fun errorResponse(
        alias: String,
        binding: FrontendPageBindingProperties,
        params: PageParams,
    ) = BindingResponse(
        alias = alias,
        data = null,
        error = true,
        meta = BindingMeta(params = metaParams(binding, params)),
    )

    private fun metaParams(
        binding: FrontendPageBindingProperties,
        params: PageParams,
    ): Map<String, String> =
        binding.overrides
            .mapNotNull { key -> params.display(key)?.let { key to it } }
            .toMap()

    private class ResolvedBinding(
        val response: BindingResponse,
        val template: String?,
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}