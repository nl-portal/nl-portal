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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageBindingProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.PortalPageProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.portalpage.PortalPageConfigValidator.Companion.DYNAMIC
import nl.nlportal.core.frontend.portalpage.PortalPageConfigValidator.Companion.BINDING_SOURCE
import nl.nlportal.core.frontend.portalpage.PortalPageConfigValidator.Companion.STATIC
import nl.nlportal.core.frontend.portalpage.domain.PortalPageBindingMeta
import nl.nlportal.core.frontend.portalpage.domain.PortalPageBindingResponse
import nl.nlportal.core.frontend.portalpage.domain.PortalPageResponse
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingData
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateLoader
import nl.nlportal.core.util.CoreUtils
import kotlin.coroutines.cancellation.CancellationException

/**
 * Serves the configured `nl-portal.frontend.pages` to users who can reach them through a `portal-page` menu entry.
 * Every request validates the page and its bindings; a problem with one binding never affects the others.
 * Configuration problems aren't logged here ([PortalPagesStartupValidation] does that); failing resolvers and template sources are.
 */
class PortalPageService(
    private val properties: FrontendConfigurationProperties,
    private val menuService: FrontendMenuService,
    private val resolvers: List<PortalPageBindingResolver>,
    private val projection: PortalPageProjection,
    private val validator: PortalPageConfigValidator,
    private val templateLoader: PortalPageTemplateLoader,
) {
    /**
     * A page with its template and all bindings resolved. With an [id], every binding resolves that object instead of a list.
     *
     * @throws PortalPageNotFoundException when the page isn't visible or isn't valid, its template is missing or fails to load,
     * a static page is requested with an id, or any binding doesn't find [id] for the user
     */
    suspend fun getPage(
        context: PortalPageRequestContext,
        key: String,
        id: String? = null,
    ): PortalPageResponse {
        val (pageKey, page) = findPage(context, key)
        if (id != null && page.type == STATIC) throw PortalPageNotFoundException("Static page '$pageKey' has no id")
        val template = page.template!!
        val loadedTemplate = if (template.source == BINDING_SOURCE) null else loadTemplate(pageKey, template.source, template.params)

        val bindings =
            resolveBindings(pageKey, page) { alias, binding, resolver, params ->
                if (id == null) {
                    toResolved(alias, binding, params, resolver.resolve(context, params))
                } else {
                    resolver.resolve(context, params, id)?.let { toResolved(alias, binding, params, it) }
                }
            }
        if (bindings.any { it == null }) throw PortalPageNotFoundException("Object not found for page '$pageKey'")
        val resolved = bindings.map { it!! }

        val root = template.binding.takeIf { template.source == BINDING_SOURCE }
        return PortalPageResponse(
            type = page.type,
            template = if (root == null) loadedTemplate!!.template else resolved.single { it.response.alias == root }.template,
            messages = loadedTemplate?.messages.orEmpty(),
            root = root,
            bindings = resolved.map { it.response },
        )
    }

    private suspend fun loadTemplate(
        pageKey: String,
        source: String,
        params: Map<String, String>,
    ): PortalPageTemplate {
        val loaded =
            try {
                templateLoader.load(source, params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure(e) { "Template of portal page '$pageKey' (source '$source') failed to load" }
                throw PortalPageNotFoundException("Template of page '$pageKey' failed to load")
            }
        if (loaded == null) {
            logger.warn { "Template of portal page '$pageKey' (source '$source') not found; the page answers 404" }
            throw PortalPageNotFoundException("Template of page '$pageKey' not found")
        }
        return loaded
    }

    /**
     * Resolves binding [alias] of a dynamic page again with [overrides] applied to its params.
     * A blank override value resets the key to its configured value.
     *
     * @throws PortalPageNotFoundException when the page or binding isn't visible, isn't valid or the page isn't dynamic
     * @throws PortalPageOverrideException when an override key isn't allowed or the merged params are invalid
     */
    suspend fun refetch(
        context: PortalPageRequestContext,
        key: String,
        alias: String,
        overrides: Map<String, String>,
    ): PortalPageBindingResponse {
        val (pageKey, page) = findPage(context, key)
        if (page.type != DYNAMIC) throw PortalPageNotFoundException("Page '$pageKey' is not a dynamic page")
        val binding = page.bindings[alias] ?: throw PortalPageNotFoundException("Page '$pageKey' has no binding '$alias'")
        if (validator.bindingProblems(alias, binding).isNotEmpty()) throw PortalPageNotFoundException("Binding '$alias' of page '$pageKey' is invalid")

        val allowed = binding.overrides.map { PortalPageParams.canonical(it) }.toSet()
        val notAllowed = overrides.keys.filter { PortalPageParams.canonical(it) !in allowed }
        if (notAllowed.isNotEmpty()) throw PortalPageOverrideException("Params not allowed: ${notAllowed.joinToString()}")

        val params = PortalPageParams(binding.params).withOverrides(overrides)
        val resolver = resolverFor(binding)
        val problems = validator.paramProblems(resolver, params)
        if (problems.isNotEmpty()) throw PortalPageOverrideException("Invalid params: ${problems.joinToString()}")

        return try {
            toResolved(alias, binding, params, resolver.resolve(context, params)).response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure(e) { "Binding '$alias' of page '$pageKey' (resolver '${binding.resolver}') failed" }
            errorResponse(alias, binding, params)
        }
    }

    private suspend fun findPage(
        context: PortalPageRequestContext,
        key: String,
    ): Pair<String, PortalPageProperties> {
        val matches = properties.pages.entries.filter { PortalPageParams.canonical(it.key) == PortalPageParams.canonical(key) }
        val (pageKey, page) = matches.singleOrNull() ?: throw PortalPageNotFoundException("Page '$key' not found")
        if (!menuService.isPortalPageVisible(context.authentication, pageKey)) throw PortalPageNotFoundException("Page '$key' not visible")
        if (validator.pageProblems(pageKey, page, properties.pages.keys).isNotEmpty()) throw PortalPageNotFoundException("Page '$key' is invalid")
        return pageKey to page
    }

    private suspend fun resolveBindings(
        pageKey: String,
        page: PortalPageProperties,
        resolve: suspend (String, PortalPageBindingProperties, PortalPageBindingResolver, PortalPageParams) -> ResolvedBinding?,
    ): List<ResolvedBinding?> =
        coroutineScope {
            page.bindings.entries
                .sortedBy { it.key }
                .map { (alias, binding) ->
                    async {
                        val params = PortalPageParams(binding.params)
                        if (validator.bindingProblems(alias, binding).isNotEmpty()) return@async ResolvedBinding(errorResponse(alias, binding, params), null)
                        try {
                            resolve(alias, binding, resolverFor(binding), params)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logFailure(e) { "Binding '$alias' of page '$pageKey' (resolver '${binding.resolver}') failed" }
                            ResolvedBinding(errorResponse(alias, binding, params), null)
                        }
                    }
                }.awaitAll()
        }

    private fun resolverFor(binding: PortalPageBindingProperties): PortalPageBindingResolver = resolvers.single { it.type == binding.resolver }

    private fun toResolved(
        alias: String,
        binding: PortalPageBindingProperties,
        params: PortalPageParams,
        bindingData: PortalPageBindingData,
    ) = ResolvedBinding(
        response =
            PortalPageBindingResponse(
                alias = alias,
                data = projection.apply(binding.projection, bindingData.data),
                error = false,
                meta = PortalPageBindingMeta(params = metaParams(binding, params)),
            ),
        template = bindingData.template,
    )

    private fun errorResponse(
        alias: String,
        binding: PortalPageBindingProperties,
        params: PortalPageParams,
    ) = PortalPageBindingResponse(
        alias = alias,
        data = null,
        error = true,
        meta = PortalPageBindingMeta(params = metaParams(binding, params)),
    )

    private fun metaParams(
        binding: PortalPageBindingProperties,
        params: PortalPageParams,
    ): Map<String, String> =
        binding.overrides
            .mapNotNull { key -> params.display(key)?.let { key to it } }
            .toMap()

    /**
     * Logs a failing resolver or template source: a WARN without the exception message, which can hold a request URL with
     * identifiers such as a BSN, and the full exception at TRACE.
     */
    private fun logFailure(
        e: Exception,
        message: () -> String,
    ) {
        logger.warn { "${message()}: ${CoreUtils.logSummary(e)}" }
        logger.trace(e) { message() }
    }

    private class ResolvedBinding(
        val response: PortalPageBindingResponse,
        val template: String?,
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}