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

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.PortalPagesConfigurationProperties
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.ByteArrayResource

/**
 * The same page configuration from YAML, a config server, env vars and the config panel
 * must give the same page model once params are read through [PortalPageParams].
 */
internal class PortalPagePropertiesBindingTest {
    @Test
    fun `every config source gives the same page model`() {
        val yaml = model(yaml())

        assertEquals(yaml, model(configServer()), "config server")
        assertEquals(yaml, model(environment()), "env vars")
        assertEquals(yaml, model(configPanel()), "config panel")
    }

    @Test
    fun `dotted param keys stay flat and lists read the same from every source`() {
        listOf(yaml(), configServer(), environment(), configPanel()).forEach { source ->
            val binding =
                bind(source)
                    .pages.entries
                    .single { PortalPageParams.canonical(it.key) == "erfpacht" }
                    .value.bindings
                    .getValue("producten")
            val params = PortalPageParams(binding.params)

            assertEquals("actief", params.string("filter.status"), source.name)
            assertEquals(mapOf("status" to "actief"), params.withPrefix("filter"), source.name)
            assertEquals(listOf("erfpacht", "woonwagen"), params.list("product-type-codes"), source.name)
            assertEquals("erfpacht", params.string("PRODUCTTYPECODE"), source.name)
            assertEquals(0, params.int("page"), source.name)
            assertEquals(listOf("page", "filter.status"), binding.overrides.map { PortalPageParams.canonical(it) }, source.name)
        }
    }

    @Test
    fun `template locations read the same from every source`() {
        listOf(yaml(), configServer(), environment(), configPanel()).forEach { source ->
            val portalPages =
                Binder(ConfigurationPropertySource.from(source)!!)
                    .bind("nl-portal.config.portal-pages", Bindable.of(PortalPagesConfigurationProperties::class.java))
                    .get()
            assertEquals(listOf("file:/etc/nl-portal/portal-pages/", "classpath:portal-pages/"), portalPages.properties.templateLocations.map { it.trim() }, source.name)
        }
    }

    @Test
    fun `page key from env vars matches the menu page key`() {
        val keys = bind(environment()).pages.keys

        assertEquals(setOf("erfpacht", "overons", "product"), keys)
        assertEquals(PortalPageParams.canonical("over-ons"), keys.single { it.startsWith("over") })
    }

    private fun model(source: PropertySource<*>): Map<String, Any?> =
        bind(source)
            .pages.entries
            .associate { (key, page) ->
                PortalPageParams.canonical(key) to
                    listOf(
                        page.type,
                        page.template?.let { listOf(it.source, it.binding, normalised(it.params)) },
                        page.bindings.entries.sortedBy { it.key }.map { (alias, binding) ->
                            listOf(
                                alias,
                                binding.resolver,
                                normalised(binding.params),
                                binding.overrides.map { PortalPageParams.canonical(it) },
                                binding.projection,
                            )
                        },
                    )
            }.toSortedMap()

    private fun normalised(values: Map<String, String>): Map<String, String?> {
        val params = PortalPageParams(values)
        return values.keys
            .map { PortalPageParams.canonical(it).replace(INDEX_SUFFIX, "") }
            .distinct()
            .associateWith { params.display(it) }
            .filterValues { it != null }
            .toSortedMap()
    }

    private fun bind(source: PropertySource<*>): FrontendConfigurationProperties =
        Binder(ConfigurationPropertySource.from(source)!!)
            .bind("nl-portal.frontend", Bindable.of(FrontendConfigurationProperties::class.java))
            .get()

    private fun yaml(): PropertySource<*> =
        YamlPropertySourceLoader()
            .load(
                "yaml",
                ByteArrayResource(
                    """
                    nl-portal:
                      config:
                        portal-pages:
                          properties:
                            template-locations: [file:/etc/nl-portal/portal-pages/, classpath:portal-pages/]
                      frontend:
                        pages:
                          over-ons:
                            type: static
                            template: { source: file, params: { file: over-ons.portal-page-template.json } }
                          erfpacht:
                            type: dynamic
                            template: { source: file, params: { file: erfpacht.portal-page-template.json } }
                            bindings:
                              producten:
                                resolver: openproduct
                                params:
                                  product-type-codes: [erfpacht, woonwagen]
                                  product-type-code: erfpacht
                                  filter.status: actief
                                  page: 0
                                  size: 10
                                  reset: ~
                                overrides: [page, filter.status]
                                projection: '[.results[] | {id: .uuid, naam, status, prijs}]'
                          product:
                            type: dynamic
                            template: { source: binding, binding: product }
                            bindings:
                              product:
                                resolver: openproduct
                                params:
                                  content-label: klantportaal
                                projection: '{naam, start_datum, eind_datum, prijs, dataobject}'
                    """.trimIndent().toByteArray(),
                ),
            ).single()

    private fun configServer(): PropertySource<*> =
        MapPropertySource(
            "config server",
            mapOf(
                "$TEMPLATES.template-locations[0]" to "file:/etc/nl-portal/portal-pages/",
                "$TEMPLATES.template-locations[1]" to "classpath:portal-pages/",
                "$PAGES.over-ons.type" to "static",
                "$PAGES.over-ons.template.source" to "file",
                "$PAGES.over-ons.template.params.file" to "over-ons.portal-page-template.json",
                "$PAGES.erfpacht.type" to "dynamic",
                "$PAGES.erfpacht.template.source" to "file",
                "$PAGES.erfpacht.template.params.file" to "erfpacht.portal-page-template.json",
                "$PRODUCTEN.resolver" to "openproduct",
                "$PRODUCTEN.params.product-type-codes[0]" to "erfpacht",
                "$PRODUCTEN.params.product-type-codes[1]" to "woonwagen",
                "$PRODUCTEN.params.product-type-code" to "erfpacht",
                "$PRODUCTEN.params[filter.status]" to "actief",
                "$PRODUCTEN.params.page" to 0,
                "$PRODUCTEN.params.size" to 10,
                "$PRODUCTEN.params.reset" to "",
                "$PRODUCTEN.overrides[0]" to "page",
                "$PRODUCTEN.overrides[1]" to "filter.status",
                "$PRODUCTEN.projection" to "[.results[] | {id: .uuid, naam, status, prijs}]",
                "$PAGES.product.type" to "dynamic",
                "$PAGES.product.template.source" to "binding",
                "$PAGES.product.template.binding" to "product",
                "$PAGES.product.bindings.product.resolver" to "openproduct",
                "$PAGES.product.bindings.product.params.content-label" to "klantportaal",
                "$PAGES.product.bindings.product.projection" to "{naam, start_datum, eind_datum, prijs, dataobject}",
            ),
        )

    private fun environment(): PropertySource<*> =
        SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            mapOf(
                "NLPORTAL_CONFIG_PORTALPAGES_PROPERTIES_TEMPLATELOCATIONS_0" to "file:/etc/nl-portal/portal-pages/",
                "NLPORTAL_CONFIG_PORTALPAGES_PROPERTIES_TEMPLATELOCATIONS_1" to "classpath:portal-pages/",
                "NLPORTAL_FRONTEND_PAGES_OVERONS_TYPE" to "static",
                "NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_SOURCE" to "file",
                "NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_PARAMS_FILE" to "over-ons.portal-page-template.json",
                "NLPORTAL_FRONTEND_PAGES_ERFPACHT_TYPE" to "dynamic",
                "NLPORTAL_FRONTEND_PAGES_ERFPACHT_TEMPLATE_SOURCE" to "file",
                "NLPORTAL_FRONTEND_PAGES_ERFPACHT_TEMPLATE_PARAMS_FILE" to "erfpacht.portal-page-template.json",
                "${ENV_PRODUCTEN}_RESOLVER" to "openproduct",
                "${ENV_PRODUCTEN}_PARAMS_PRODUCTTYPECODES_1" to "woonwagen",
                "${ENV_PRODUCTEN}_PARAMS_PRODUCTTYPECODES_0" to "erfpacht",
                "${ENV_PRODUCTEN}_PARAMS_PRODUCTTYPECODE" to "erfpacht",
                "${ENV_PRODUCTEN}_PARAMS_FILTER_STATUS" to "actief",
                "${ENV_PRODUCTEN}_PARAMS_PAGE" to "0",
                "${ENV_PRODUCTEN}_PARAMS_SIZE" to "10",
                "${ENV_PRODUCTEN}_OVERRIDES_0" to "page",
                "${ENV_PRODUCTEN}_OVERRIDES_1" to "filter.status",
                "${ENV_PRODUCTEN}_PROJECTION" to "[.results[] | {id: .uuid, naam, status, prijs}]",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_TYPE" to "dynamic",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_TEMPLATE_SOURCE" to "binding",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_TEMPLATE_BINDING" to "product",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_BINDINGS_PRODUCT_RESOLVER" to "openproduct",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_BINDINGS_PRODUCT_PARAMS_CONTENTLABEL" to "klantportaal",
                "NLPORTAL_FRONTEND_PAGES_PRODUCT_BINDINGS_PRODUCT_PROJECTION" to "{naam, start_datum, eind_datum, prijs, dataobject}",
            ),
        )

    private fun configPanel(): PropertySource<*> =
        MapPropertySource(
            "config panel",
            mapOf(
                "$TEMPLATES.template-locations" to "file:/etc/nl-portal/portal-pages/, classpath:portal-pages/",
                "$PAGES.over-ons.type" to "static",
                "$PAGES.over-ons.template.source" to "file",
                "$PAGES.over-ons.template.params.file" to "over-ons.portal-page-template.json",
                "$PAGES.erfpacht.type" to "dynamic",
                "$PAGES.erfpacht.template.source" to "file",
                "$PAGES.erfpacht.template.params.file" to "erfpacht.portal-page-template.json",
                "$PRODUCTEN.resolver" to "openproduct",
                "$PRODUCTEN.params.product-type-codes" to "erfpacht, woonwagen",
                "$PRODUCTEN.params.product-type-code" to "erfpacht",
                "$PRODUCTEN.params.filter.status" to "actief",
                "$PRODUCTEN.params.page" to "0",
                "$PRODUCTEN.params.size" to "10",
                "$PRODUCTEN.overrides" to "page, filter.status",
                "$PRODUCTEN.projection" to "[.results[] | {id: .uuid, naam, status, prijs}]",
                "$PAGES.product.type" to "dynamic",
                "$PAGES.product.template.source" to "binding",
                "$PAGES.product.template.binding" to "product",
                "$PAGES.product.bindings.product.resolver" to "openproduct",
                "$PAGES.product.bindings.product.params.content-label" to "klantportaal",
                "$PAGES.product.bindings.product.projection" to "{naam, start_datum, eind_datum, prijs, dataobject}",
            ),
        )

    companion object {
        private val INDEX_SUFFIX = Regex("\\.\\d+$")
        private const val PAGES = "nl-portal.frontend.pages"
        private const val TEMPLATES = "nl-portal.config.portal-pages.properties"
        private const val PRODUCTEN = "$PAGES.erfpacht.bindings.producten"
        private const val ENV_PRODUCTEN = "NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN"
    }
}