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
package nl.nlportal.core.frontend.page.template

import nl.nlportal.core.util.Mapper
import tools.jackson.databind.JsonNode

/**
 * The `PortaalPaginaTemplate` contract, schema 1.0.0 (`documentation/features/portaal-pagina-template.schema.json`):
 * `naam` for editors, `template` and optional `messages` per portal locale. The format of `.pagetemplate.json` files
 * and of `PortaalPaginaTemplate` objects in the Objects API alike, so templates move between sources unchanged.
 */
object PortaalPaginaTemplate {
    private val FIELDS = setOf("naam", "template", "messages")

    /**
     * @throws Exception when [json] isn't valid JSON or doesn't follow the contract
     */
    fun parse(json: String): PageTemplateDocument = parse(Mapper.get().readTree(json))

    /**
     * @throws IllegalArgumentException when [data] doesn't follow the contract
     */
    fun parse(data: JsonNode): PageTemplateDocument {
        require(data.isObject) { "a PortaalPaginaTemplate must be an object" }
        val unknown = data.propertyNames().filter { it !in FIELDS }
        require(unknown.isEmpty()) { "unknown PortaalPaginaTemplate fields: ${unknown.joinToString()}" }
        val naam = data.path("naam")
        require(naam.isString && naam.stringValue().isNotBlank()) { "naam must be a non-empty string" }
        val template = data.path("template")
        require(template.isString) { "template must be a string" }
        val messages = data.get("messages")?.takeUnless { it.isNull }?.let { Mapper.get().convertValue(it, Any::class.java) }
        return PageTemplateDocument.of(template.stringValue(), messages)
    }
}