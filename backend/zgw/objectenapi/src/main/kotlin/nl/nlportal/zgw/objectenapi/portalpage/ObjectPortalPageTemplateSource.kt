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
package nl.nlportal.zgw.objectenapi.portalpage

import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateSource
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.JsonNode
import java.net.URI
import java.util.UUID

/**
 * Loads portal page templates from `PortaalPaginaTemplate` objects in the configured Objecten API, by object URL
 * (`object-url`, as Open Object shows it). An URL on another host than `nl-portal.config.objectenapi.url` is invalid
 * and never requested. An object of another objecttype counts as missing; objecttypes are compared by UUID only, because
 * the Objecttypen API can live on another host and objects may refer to it by an internal URL.
 */
class ObjectPortalPageTemplateSource(
    private val objectenApiService: ObjectenApiService,
    private val properties: ObjectPortalPageTemplateSourceProperties,
) : PortalPageTemplateSource {
    override val type = "object"

    override fun validate(params: PortalPageParams): List<String> =
        buildList {
            when {
                properties.objectTypeUrl.isBlank() -> add("$OBJECT_TYPE_URL_PROPERTY is not configured")
                objectTypeId(properties.objectTypeUrl) == null -> add("$OBJECT_TYPE_URL_PROPERTY must end with the objecttype UUID")
            }
            val url = params.string(OBJECT_URL_PARAM)?.let { runCatching { URI(it) }.getOrNull() }
            when {
                url == null || !url.isAbsolute || url.scheme !in SCHEMES || url.host == null -> {
                    add("param '$OBJECT_URL_PARAM' must be an absolute http(s) URL")
                }

                !url.host.equals(objectenApiService.objectsApiClientConfig.url.host, ignoreCase = true) -> {
                    add("param '$OBJECT_URL_PARAM' must be on the host of nl-portal.config.objectenapi.url")
                }
            }
        }

    override suspend fun load(params: PortalPageParams): PortalPageTemplate? {
        require(validate(params).isEmpty()) { validate(params).joinToString() }
        val templateObject =
            try {
                objectenApiService.getObjectByUrl<JsonNode>(params.string(OBJECT_URL_PARAM)!!)
            } catch (e: WebClientResponseException.NotFound) {
                null
            } ?: return null
        if (objectTypeId(templateObject.type) != objectTypeId(properties.objectTypeUrl)) return null
        return PortalPageTemplate.parse(templateObject.record.data)
    }

    private fun objectTypeId(url: String): UUID? =
        runCatching {
            UUID.fromString(
                URI(url.trim())
                    .path
                    .trimEnd('/')
                    .substringAfterLast('/'),
            )
        }.getOrNull()

    companion object {
        private const val OBJECT_URL_PARAM = "object-url"
        private const val OBJECT_TYPE_URL_PROPERTY = "nl-portal.config.portal-pages.properties.object.object-type-url"
        private val SCHEMES = setOf("http", "https")
    }
}