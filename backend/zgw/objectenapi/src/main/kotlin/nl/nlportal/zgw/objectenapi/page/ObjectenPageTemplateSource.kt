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
package nl.nlportal.zgw.objectenapi.page

import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.core.frontend.page.template.PageTemplateSource
import nl.nlportal.core.frontend.page.template.PortaalPaginaTemplate
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.JsonNode
import java.util.UUID

/**
 * Loads page templates from `PortaalPaginaTemplate` objects in the Objects API, by object UUID.
 * Objects of another objecttype than the configured one count as missing.
 */
class ObjectenPageTemplateSource(
    private val objectenApiService: ObjectenApiService,
    private val properties: ObjectenPageTemplateProperties,
) : PageTemplateSource {
    override val type = "objecten"

    override fun validate(params: PageParams): List<String> =
        buildList {
            if (properties.objectTypeUrl.isBlank()) add("nl-portal.config.pages.properties.objecten.object-type-url is not configured")
            val id = params.string(OBJECT_PARAM)
            if (id == null || runCatching { UUID.fromString(id) }.isFailure) add("param '$OBJECT_PARAM' must be a UUID")
        }

    override suspend fun load(params: PageParams): PageTemplateDocument? {
        require(validate(params).isEmpty()) { validate(params).joinToString() }
        val id = params.string(OBJECT_PARAM)!!
        val templateObject =
            try {
                objectenApiService.getObjectById<JsonNode>(id)
            } catch (e: ResponseStatusException) {
                if (e.statusCode.value() == HttpStatus.NOT_FOUND.value()) return null
                throw e
            } ?: return null
        if (templateObject.type.trimEnd('/') != properties.objectTypeUrl.trimEnd('/')) return null
        return PortaalPaginaTemplate.parse(templateObject.record.data)
    }

    companion object {
        private const val OBJECT_PARAM = "object"
    }
}